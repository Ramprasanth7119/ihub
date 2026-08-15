package com.ihub.service;

import com.ihub.dao.AuctionDao;
import com.ihub.dao.AuctionEventDao;
import com.ihub.dao.AuctionLifecycleDao;
import com.ihub.dao.BidDao;
import com.ihub.dao.IdeaDao;
import com.ihub.exception.ConflictException;
import com.ihub.exception.NotFoundException;
import com.ihub.model.Auction;
import com.ihub.model.AuctionStatus;
import com.ihub.model.Idea;
import com.ihub.notification.event.AuctionCancelledEvent;
import com.ihub.notification.event.AuctionEndedEvent;
import com.ihub.notification.event.AuctionEndingSoonEvent;
import com.ihub.notification.event.AuctionStartedEvent;
import com.ihub.notification.event.WinnerAnnouncedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Owns every auction state transition: {@code SCHEDULED → ACTIVE → CLOSED}, plus
 * cancellation out of either non-terminal state.
 *
 * <p>All entry points — the scheduler, the creator-facing {@link AuctionService} and
 * the admin console — funnel through here so that winner selection, auction-event
 * history, Elasticsearch synchronisation and notifications happen exactly once per
 * transition, no matter who triggered it.</p>
 */
@Service
public class AuctionLifecycleService {

    private static final Logger log = LoggerFactory.getLogger(AuctionLifecycleService.class);

    private final AuctionDao auctionDao;
    private final AuctionLifecycleDao lifecycleDao;
    private final AuctionEventDao eventDao;
    private final IdeaDao ideaDao;
    private final BidDao bidDao;
    private final IdeaSearchService ideaSearchService;
    private final ApplicationEventPublisher eventPublisher;
    private final int endingSoonWindowMinutes;

    public AuctionLifecycleService(
            AuctionDao auctionDao,
            AuctionLifecycleDao lifecycleDao,
            AuctionEventDao eventDao,
            IdeaDao ideaDao,
            BidDao bidDao,
            IdeaSearchService ideaSearchService,
            ApplicationEventPublisher eventPublisher,
            @Value("${auction.ending-soon-window-minutes:15}") int endingSoonWindowMinutes) {
        this.endingSoonWindowMinutes = endingSoonWindowMinutes;
        this.auctionDao = auctionDao;
        this.lifecycleDao = lifecycleDao;
        this.eventDao = eventDao;
        this.ideaDao = ideaDao;
        this.bidDao = bidDao;
        this.ideaSearchService = ideaSearchService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Advances every auction whose scheduled moment has arrived.
     *
     * @param autoStart whether auctions may transition SCHEDULED → ACTIVE automatically
     * @param autoEnd   whether auctions may transition ACTIVE → CLOSED automatically
     */
    @Transactional
    public void processDueAuctions(boolean autoStart, boolean autoEnd) {
        List<Auction> toStart = autoStart ? lifecycleDao.findDueToStart() : List.of();
        for (Auction auction : toStart) {
            startAuctionInternal(auction, "STARTED", "Auction auto-started at scheduled time");
        }

        List<Auction> endingSoon = lifecycleDao.findEndingSoon(endingSoonWindowMinutes);
        for (Auction auction : endingSoon) {
            notifyEndingSoon(auction);
        }

        List<Auction> toClose = autoEnd ? lifecycleDao.findDueToClose() : List.of();
        for (Auction auction : toClose) {
            closeAuctionInternal(auction, "CLOSED", "Auction auto-closed at scheduled end time");
        }

        if (!toStart.isEmpty() || !toClose.isEmpty() || !endingSoon.isEmpty()) {
            log.info("Auction lifecycle processed: started={}, endingSoon={}, closed={}",
                    toStart.size(), endingSoon.size(), toClose.size());
        }
    }

    /** Warns the creator and every existing bidder that an auction is about to close. */
    private void notifyEndingSoon(Auction auction) {
        // The row was selected by a SQL window predicate, so remaining time is known
        // to be within the configured window. Clamping to it keeps the wording sane
        // even if the JVM clock is offset from the database server's.
        long minutesRemaining = Math.min(endingSoonWindowMinutes,
                Math.max(1, Duration.between(LocalDateTime.now(), auction.getEndTime()).toMinutes()));

        // Recorded before publishing so a retry cannot double-notify.
        eventDao.recordEvent(auction.getId(), "ENDING_SOON",
                "Auction closing in approximately " + minutesRemaining + " minute(s)");

        Idea idea = ideaDao.getIdeaById(auction.getIdeaId());
        eventPublisher.publishEvent(new AuctionEndingSoonEvent(
                auction.getId(),
                auction.getIdeaId(),
                idea.getCreatorId(),
                idea.getTitle(),
                bidDao.findDistinctBidderIds(auction.getId()),
                minutesRemaining
        ));
    }

    @Transactional
    public Auction startAuction(Long auctionId, boolean manual) {
        Auction auction = getAuctionOrThrow(auctionId);
        assertTransitionAllowed(auction, AuctionStatus.ACTIVE);

        String eventType = manual ? "MANUAL_START" : "STARTED";
        String details = manual ? "Auction manually started" : "Auction started";
        startAuctionInternal(auction, eventType, details);

        return getAuctionOrThrow(auctionId);
    }

    @Transactional
    public Auction closeAuction(Long auctionId, boolean manual) {
        Auction auction = getAuctionOrThrow(auctionId);
        assertTransitionAllowed(auction, AuctionStatus.CLOSED);

        String eventType = manual ? "MANUAL_CLOSE" : "CLOSED";
        String details = manual ? "Auction manually closed" : "Auction closed";
        closeAuctionInternal(auction, eventType, details);

        return getAuctionOrThrow(auctionId);
    }

    /**
     * Cancels a scheduled or running auction. No winner is recorded — a cancelled
     * auction has no outcome — but anyone who already bid is notified, since they
     * may have been relying on it.
     */
    @Transactional
    public Auction cancelAuction(Long auctionId, String reason) {
        Auction auction = getAuctionOrThrow(auctionId);
        assertTransitionAllowed(auction, AuctionStatus.CANCELLED);

        int updated = lifecycleDao.cancelById(auctionId);
        if (updated == 0) {
            // Another transaction moved it to a terminal state first.
            throw new ConflictException("Auction is no longer cancellable");
        }

        String details = reason != null && !reason.isBlank()
                ? "Auction cancelled: " + reason
                : "Auction cancelled";
        eventDao.recordEvent(auctionId, "CANCELLED", details);
        ideaSearchService.updateStatus(auction.getIdeaId(), "CANCELLED");

        Idea idea = ideaDao.getIdeaById(auction.getIdeaId());
        List<Long> bidderIds = bidDao.findDistinctBidderIds(auctionId);
        eventPublisher.publishEvent(new AuctionCancelledEvent(
                auctionId,
                auction.getIdeaId(),
                idea.getCreatorId(),
                idea.getTitle(),
                bidderIds
        ));

        return getAuctionOrThrow(auctionId);
    }

    public void recordScheduledEvent(Long auctionId) {
        eventDao.recordEvent(auctionId, "SCHEDULED", "Auction created and scheduled");
    }

    /**
     * Guards the state machine. Transitions are only legal from the states listed
     * below; everything else is a 409 rather than a silent no-op.
     */
    private void assertTransitionAllowed(Auction auction, AuctionStatus target) {
        String current = auction.getStatus();

        switch (target) {
            case ACTIVE -> {
                if (!AuctionStatus.SCHEDULED.matches(current)) {
                    throw new ConflictException(
                            "Only scheduled auctions can be started (current status: " + current + ")");
                }
            }
            case CLOSED -> {
                if (!AuctionStatus.ACTIVE.matches(current)) {
                    throw new ConflictException(
                            "Only active auctions can be closed (current status: " + current + ")");
                }
            }
            case CANCELLED -> {
                if (AuctionStatus.CLOSED.matches(current) || AuctionStatus.CANCELLED.matches(current)) {
                    throw new ConflictException(
                            "Auction is already " + current.toLowerCase() + " and cannot be cancelled");
                }
            }
            default -> throw new ConflictException("Unsupported auction transition: " + target);
        }
    }

    private void startAuctionInternal(Auction auction, String eventType, String details) {
        // A manual start means "open for bidding now", so the scheduled start time is
        // pulled forward; the scheduler's own sweep leaves it untouched.
        boolean manual = eventType.startsWith("MANUAL");
        int updated = manual
                ? lifecycleDao.activateNowById(auction.getId())
                : lifecycleDao.activateById(auction.getId());

        if (updated == 0) {
            // Already transitioned by a concurrent scheduler run — nothing to do.
            return;
        }

        eventDao.recordEvent(auction.getId(), eventType, details);
        ideaSearchService.updateStatus(auction.getIdeaId(), "ACTIVE");

        Idea idea = ideaDao.getIdeaById(auction.getIdeaId());
        eventPublisher.publishEvent(new AuctionStartedEvent(
                auction.getId(),
                auction.getIdeaId(),
                idea.getCreatorId(),
                idea.getTitle()
        ));
    }

    private void closeAuctionInternal(Auction auction, String eventType, String details) {
        // Flip the status first: the UPDATE takes the same row lock that
        // BidService acquires with SELECT ... FOR UPDATE, so no bid can land
        // between winner selection and closure. If the guard matches nothing,
        // another transaction closed it and already picked the winner.
        int updated = lifecycleDao.closeById(auction.getId());
        if (updated == 0) {
            return;
        }

        Optional<WinnerInfo> winner = selectAndSaveWinner(auction.getId());

        eventDao.recordEvent(auction.getId(), eventType, details);
        ideaSearchService.updateStatus(auction.getIdeaId(), "CLOSED");

        Idea idea = ideaDao.getIdeaById(auction.getIdeaId());
        List<Long> bidderIds = bidDao.findDistinctBidderIds(auction.getId());

        eventPublisher.publishEvent(new AuctionEndedEvent(
                auction.getId(),
                auction.getIdeaId(),
                idea.getCreatorId(),
                idea.getTitle(),
                bidderIds
        ));

        winner.ifPresent(w -> eventPublisher.publishEvent(new WinnerAnnouncedEvent(
                auction.getId(),
                auction.getIdeaId(),
                idea.getCreatorId(),
                w.investorId(),
                idea.getTitle(),
                w.amount()
        )));
    }

    private Optional<WinnerInfo> selectAndSaveWinner(Long auctionId) {
        if (lifecycleDao.hasWinner(auctionId)) {
            return Optional.empty();
        }

        List<Map<String, Object>> winners = lifecycleDao.findWinnersForAuctions(List.of(auctionId));
        if (winners.isEmpty()) {
            eventDao.recordEvent(auctionId, "NO_BIDS", "Auction closed with no bids");
            return Optional.empty();
        }

        Map<String, Object> winner = winners.get(0);
        Long investorId = ((Number) winner.get("investor_id")).longValue();
        Double amount = ((Number) winner.get("bid_amount")).doubleValue();

        lifecycleDao.saveWinner(auctionId, investorId, amount);
        eventDao.recordEvent(
                auctionId,
                "WINNER_SELECTED",
                "Winner investorId=" + investorId + " bid=" + amount
        );
        return Optional.of(new WinnerInfo(investorId, amount));
    }

    private Auction getAuctionOrThrow(Long auctionId) {
        try {
            return auctionDao.getAuctionById(auctionId);
        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException("Auction not found");
        }
    }

    private record WinnerInfo(Long investorId, Double amount) {
    }
}
