package com.ihub.service;

import com.ihub.dao.BidDao;
import com.ihub.dao.UserDao;
import com.ihub.dto.BidRequest;
import com.ihub.dto.BidResponse;
import com.ihub.exception.ConflictException;
import com.ihub.exception.ForbiddenException;
import com.ihub.exception.NotFoundException;
import com.ihub.exception.UnprocessableEntityException;
import com.ihub.model.AuctionBidContext;
import com.ihub.model.User;
import com.ihub.notification.event.OutbidEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Bidding rules: auction window, role, self-bid, and minimum-increment enforcement.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BidServiceTest {

    private static final Long AUCTION_ID = 42L;
    private static final Long INVESTOR_ID = 7L;
    private static final Long CREATOR_ID = 3L;

    @Mock private BidDao bidDao;
    @Mock private UserDao userDao;
    @Mock private BidBroadcastService broadcastService;
    @Mock private ApplicationEventPublisher eventPublisher;

    private BidService bidService;

    @BeforeEach
    void setUp() {
        bidService = new BidService(bidDao, userDao, broadcastService, eventPublisher);
        authenticateAs("investor@ihub.test", "INVESTOR", INVESTOR_ID);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("a bid on a missing auction is 404, not 400")
    void missingAuctionIsNotFound() {
        when(bidDao.lockAuctionForBid(AUCTION_ID)).thenReturn(null);

        assertThatThrownBy(() -> bidService.placeBid(request(1000)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("bidding before the auction opens is rejected")
    void rejectsBidBeforeStart() {
        AuctionBidContext context = context("ACTIVE");
        context.setBeforeStart(true);
        when(bidDao.lockAuctionForBid(AUCTION_ID)).thenReturn(context);

        assertThatThrownBy(() -> bidService.placeBid(request(200_000)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("not started");

        verify(bidDao, never()).saveBid(anyLong(), anyLong(), any());
    }

    @Test
    @DisplayName("bidding past the end time is rejected even while the status still reads ACTIVE")
    void rejectsBidAfterEndTimeEvenIfStillActive() {
        // The lifecycle scheduler only sweeps once a minute, so this is the exact
        // window in which an auction is past its end time but not yet marked CLOSED.
        AuctionBidContext context = context("ACTIVE");
        context.setAfterEnd(true);
        when(bidDao.lockAuctionForBid(AUCTION_ID)).thenReturn(context);

        assertThatThrownBy(() -> bidService.placeBid(request(200_000)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already closed");

        verify(bidDao, never()).saveBid(anyLong(), anyLong(), any());
    }

    @Test
    @DisplayName("a cancelled auction accepts no bids")
    void rejectsBidOnCancelledAuction() {
        when(bidDao.lockAuctionForBid(AUCTION_ID)).thenReturn(context("CANCELLED"));

        assertThatThrownBy(() -> bidService.placeBid(request(200_000)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("cancelled");
    }

    @Test
    @DisplayName("a creator cannot bid on their own idea")
    void rejectsSelfBid() {
        authenticateAs("creator@ihub.test", "INVESTOR", CREATOR_ID);
        when(bidDao.lockAuctionForBid(AUCTION_ID)).thenReturn(context("ACTIVE"));

        assertThatThrownBy(() -> bidService.placeBid(request(200_000)))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("own ideas");
    }

    @Test
    @DisplayName("only investors may bid")
    void rejectsNonInvestor() {
        authenticateAs("creator@ihub.test", "CREATOR", CREATOR_ID);

        assertThatThrownBy(() -> bidService.placeBid(request(200_000)))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Only investors");
    }

    @Test
    @DisplayName("the first bid must reach the idea's base price")
    void firstBidMustMeetBasePrice() {
        when(bidDao.lockAuctionForBid(AUCTION_ID)).thenReturn(context("ACTIVE"));
        when(bidDao.getHighestBid(AUCTION_ID)).thenReturn(null);

        assertThatThrownBy(() -> bidService.placeBid(request(99_999)))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining("at least");
    }

    @Test
    @DisplayName("a later bid must clear the current highest by the full increment")
    void subsequentBidMustClearIncrement() {
        when(bidDao.lockAuctionForBid(AUCTION_ID)).thenReturn(context("ACTIVE"));
        when(bidDao.getHighestBid(AUCTION_ID)).thenReturn(150_000.0);

        // 150_000 + 5_000 increment = 155_000 required; 154_999 must fail.
        assertThatThrownBy(() -> bidService.placeBid(request(154_999)))
                .isInstanceOf(UnprocessableEntityException.class);
    }

    @Test
    @DisplayName("a valid bid is persisted, broadcast, and ranked")
    void acceptsValidBid() {
        when(bidDao.lockAuctionForBid(AUCTION_ID)).thenReturn(context("ACTIVE"));
        when(bidDao.getHighestBid(AUCTION_ID)).thenReturn(150_000.0);
        when(bidDao.saveBid(AUCTION_ID, INVESTOR_ID, 155_000.0)).thenReturn(99L);
        when(bidDao.findLeaderboard(AUCTION_ID)).thenReturn(List.of(Map.of(
                "investor_id", INVESTOR_ID,
                "investor_name", "Investor",
                "bid_amount", 155_000.0,
                "latest_bid_at", java.sql.Timestamp.valueOf("2026-01-01 10:00:00"))));

        BidResponse response = bidService.placeBid(request(155_000));

        assertThat(response.getBidId()).isEqualTo(99L);
        assertThat(response.getRank()).isEqualTo(1);
        verify(bidDao).saveBid(AUCTION_ID, INVESTOR_ID, 155_000.0);
        verify(broadcastService).broadcastBidUpdate(any());
        verify(broadcastService).broadcastLeaderboard(anyLong(), any());
    }

    @Test
    @DisplayName("the previous leader is notified when they are outbid")
    void notifiesPreviousLeader() {
        when(bidDao.lockAuctionForBid(AUCTION_ID)).thenReturn(context("ACTIVE"));
        when(bidDao.getHighestBid(AUCTION_ID)).thenReturn(150_000.0);
        when(bidDao.findLeaderInvestorId(AUCTION_ID)).thenReturn(11L);
        when(bidDao.findIdeaTitleByAuction(AUCTION_ID)).thenReturn("Some Idea");
        when(bidDao.saveBid(anyLong(), anyLong(), any())).thenReturn(100L);
        when(bidDao.findLeaderboard(AUCTION_ID)).thenReturn(List.of());

        bidService.placeBid(request(155_000));

        verify(eventPublisher).publishEvent(any(OutbidEvent.class));
    }

    @Test
    @DisplayName("re-bidding as the current leader does not fire an outbid alert")
    void doesNotNotifyWhenLeaderRebids() {
        when(bidDao.lockAuctionForBid(AUCTION_ID)).thenReturn(context("ACTIVE"));
        when(bidDao.getHighestBid(AUCTION_ID)).thenReturn(150_000.0);
        when(bidDao.findLeaderInvestorId(AUCTION_ID)).thenReturn(INVESTOR_ID);
        when(bidDao.saveBid(anyLong(), anyLong(), any())).thenReturn(100L);
        when(bidDao.findLeaderboard(AUCTION_ID)).thenReturn(List.of());

        bidService.placeBid(request(155_000));

        verify(eventPublisher, never()).publishEvent(any(OutbidEvent.class));
    }

    // ------------------------------------------------------------------ helpers

    private BidRequest request(double amount) {
        BidRequest request = new BidRequest();
        request.setAuctionId(AUCTION_ID);
        request.setAmount(amount);
        return request;
    }

    private AuctionBidContext context(String status) {
        AuctionBidContext context = new AuctionBidContext();
        context.setStatus(status);
        context.setBasePrice(100_000.0);
        context.setMinBidIncrement(5_000.0);
        context.setIdeaCreatorId(CREATOR_ID);
        context.setBeforeStart(false);
        context.setAfterEnd(false);
        return context;
    }

    private void authenticateAs(String email, String role, Long id) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setRole(role);
        when(userDao.findByEmail(email)).thenReturn(user);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null, List.of()));
    }
}
