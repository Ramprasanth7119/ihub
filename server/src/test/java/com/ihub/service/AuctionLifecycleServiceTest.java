package com.ihub.service;

import com.ihub.dao.AuctionDao;
import com.ihub.dao.AuctionEventDao;
import com.ihub.dao.AuctionLifecycleDao;
import com.ihub.dao.BidDao;
import com.ihub.dao.IdeaDao;
import com.ihub.exception.ConflictException;
import com.ihub.exception.NotFoundException;
import com.ihub.model.Auction;
import com.ihub.model.Idea;
import com.ihub.notification.event.AuctionCancelledEvent;
import com.ihub.notification.event.AuctionEndedEvent;
import com.ihub.notification.event.AuctionStartedEvent;
import com.ihub.notification.event.WinnerAnnouncedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.EmptyResultDataAccessException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The auction state machine: SCHEDULED → ACTIVE → CLOSED, with CANCELLED reachable
 * from either non-terminal state and every other transition refused.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuctionLifecycleServiceTest {

    private static final Long AUCTION_ID = 5L;
    private static final Long IDEA_ID = 9L;
    private static final Long CREATOR_ID = 2L;

    @Mock private AuctionDao auctionDao;
    @Mock private AuctionLifecycleDao lifecycleDao;
    @Mock private AuctionEventDao eventDao;
    @Mock private IdeaDao ideaDao;
    @Mock private BidDao bidDao;
    @Mock private IdeaSearchService ideaSearchService;
    @Mock private ApplicationEventPublisher eventPublisher;

    private AuctionLifecycleService service;

    @BeforeEach
    void setUp() {
        service = new AuctionLifecycleService(
                auctionDao, lifecycleDao, eventDao, ideaDao, bidDao, ideaSearchService, eventPublisher, 15);

        Idea idea = new Idea();
        idea.setId(IDEA_ID);
        idea.setCreatorId(CREATOR_ID);
        idea.setTitle("Test Idea");
        when(ideaDao.getIdeaById(IDEA_ID)).thenReturn(idea);
        when(bidDao.findDistinctBidderIds(AUCTION_ID)).thenReturn(List.of(11L, 12L));
    }

    @Test
    @DisplayName("a missing auction is reported as 404")
    void missingAuctionIsNotFound() {
        when(auctionDao.getAuctionById(AUCTION_ID)).thenThrow(new EmptyResultDataAccessException(1));

        assertThatThrownBy(() -> service.startAuction(AUCTION_ID, true))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("only a SCHEDULED auction can be started")
    void cannotStartActiveAuction() {
        when(auctionDao.getAuctionById(AUCTION_ID)).thenReturn(auction("ACTIVE"));

        assertThatThrownBy(() -> service.startAuction(AUCTION_ID, true))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Only scheduled");
    }

    @Test
    @DisplayName("only an ACTIVE auction can be closed")
    void cannotCloseScheduledAuction() {
        when(auctionDao.getAuctionById(AUCTION_ID)).thenReturn(auction("SCHEDULED"));

        assertThatThrownBy(() -> service.closeAuction(AUCTION_ID, true))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Only active");
    }

    @Test
    @DisplayName("a closed auction cannot be cancelled")
    void cannotCancelClosedAuction() {
        when(auctionDao.getAuctionById(AUCTION_ID)).thenReturn(auction("CLOSED"));

        assertThatThrownBy(() -> service.cancelAuction(AUCTION_ID, "no reason"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already closed");
    }

    @Test
    @DisplayName("a manual start pulls the start time forward so bidding opens immediately")
    void manualStartActivatesNow() {
        when(auctionDao.getAuctionById(AUCTION_ID)).thenReturn(auction("SCHEDULED"), auction("ACTIVE"));
        when(lifecycleDao.activateNowById(AUCTION_ID)).thenReturn(1);

        service.startAuction(AUCTION_ID, true);

        verify(lifecycleDao).activateNowById(AUCTION_ID);
        verify(lifecycleDao, never()).activateById(anyLong());
        verify(eventPublisher).publishEvent(any(AuctionStartedEvent.class));
        verify(ideaSearchService).updateStatus(IDEA_ID, "ACTIVE");
    }

    @Test
    @DisplayName("the scheduler's automatic start respects the configured start time")
    void scheduledStartDoesNotMoveStartTime() {
        when(lifecycleDao.findDueToStart()).thenReturn(List.of(auction("SCHEDULED")));
        when(lifecycleDao.findEndingSoon(15)).thenReturn(List.of());
        when(lifecycleDao.findDueToClose()).thenReturn(List.of());
        when(lifecycleDao.activateById(AUCTION_ID)).thenReturn(1);

        service.processDueAuctions(true, true);

        verify(lifecycleDao).activateById(AUCTION_ID);
        verify(lifecycleDao, never()).activateNowById(anyLong());
    }

    @Test
    @DisplayName("auto-start can be disabled without disabling auto-close")
    void honoursAutoStartToggle() {
        when(lifecycleDao.findEndingSoon(15)).thenReturn(List.of());
        when(lifecycleDao.findDueToClose()).thenReturn(List.of());

        service.processDueAuctions(false, true);

        verify(lifecycleDao, never()).findDueToStart();
        verify(lifecycleDao).findDueToClose();
    }

    @Test
    @DisplayName("closing selects a winner and announces it")
    void closeSelectsAndAnnouncesWinner() {
        when(auctionDao.getAuctionById(AUCTION_ID)).thenReturn(auction("ACTIVE"), auction("CLOSED"));
        when(lifecycleDao.closeById(AUCTION_ID)).thenReturn(1);
        when(lifecycleDao.hasWinner(AUCTION_ID)).thenReturn(false);
        when(lifecycleDao.findWinnersForAuctions(List.of(AUCTION_ID))).thenReturn(List.of(Map.of(
                "auction_id", AUCTION_ID, "investor_id", 11L, "bid_amount", 250_000.0)));

        service.closeAuction(AUCTION_ID, true);

        verify(lifecycleDao).saveWinner(AUCTION_ID, 11L, 250_000.0);
        verify(eventPublisher).publishEvent(any(AuctionEndedEvent.class));
        verify(eventPublisher).publishEvent(any(WinnerAnnouncedEvent.class));
        verify(ideaSearchService).updateStatus(IDEA_ID, "CLOSED");
    }

    @Test
    @DisplayName("closing an auction with no bids records NO_BIDS and announces no winner")
    void closeWithoutBidsAnnouncesNoWinner() {
        when(auctionDao.getAuctionById(AUCTION_ID)).thenReturn(auction("ACTIVE"), auction("CLOSED"));
        when(lifecycleDao.closeById(AUCTION_ID)).thenReturn(1);
        when(lifecycleDao.hasWinner(AUCTION_ID)).thenReturn(false);
        when(lifecycleDao.findWinnersForAuctions(List.of(AUCTION_ID))).thenReturn(List.of());

        service.closeAuction(AUCTION_ID, true);

        verify(eventDao).recordEvent(AUCTION_ID, "NO_BIDS", "Auction closed with no bids");
        verify(eventPublisher, never()).publishEvent(any(WinnerAnnouncedEvent.class));
    }

    @Test
    @DisplayName("a concurrent close is a no-op rather than a duplicate winner")
    void concurrentCloseIsNoOp() {
        when(auctionDao.getAuctionById(AUCTION_ID)).thenReturn(auction("ACTIVE"));
        // Another transaction already flipped the status, so the guarded UPDATE
        // matches nothing and this pass must not re-run winner selection.
        when(lifecycleDao.closeById(AUCTION_ID)).thenReturn(0);

        service.closeAuction(AUCTION_ID, true);

        verify(lifecycleDao, never()).saveWinner(anyLong(), anyLong(), any());
        verify(eventPublisher, never()).publishEvent(any(AuctionEndedEvent.class));
    }

    @Test
    @DisplayName("cancelling notifies existing bidders and records no winner")
    void cancelNotifiesBidders() {
        when(auctionDao.getAuctionById(AUCTION_ID)).thenReturn(auction("ACTIVE"), auction("CANCELLED"));
        when(lifecycleDao.cancelById(AUCTION_ID)).thenReturn(1);

        service.cancelAuction(AUCTION_ID, "duplicate listing");

        verify(eventDao).recordEvent(anyLong(), anyString(), anyString());
        verify(eventPublisher).publishEvent(any(AuctionCancelledEvent.class));
        verify(lifecycleDao, never()).saveWinner(anyLong(), anyLong(), any());
    }

    private Auction auction(String status) {
        Auction auction = new Auction();
        auction.setId(AUCTION_ID);
        auction.setIdeaId(IDEA_ID);
        auction.setStatus(status);
        auction.setStartTime(LocalDateTime.now().minusHours(1));
        auction.setEndTime(LocalDateTime.now().plusHours(1));
        return auction;
    }
}
