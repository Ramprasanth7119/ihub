package com.ihub.service;

import com.ihub.dao.AuctionDao;
import com.ihub.dao.AuctionEventDao;
import com.ihub.dao.AuctionLifecycleDao;
import com.ihub.dao.BidDao;
import com.ihub.dao.IdeaDao;
import com.ihub.exception.CustomException;
import com.ihub.model.Auction;
import com.ihub.model.Idea;
import com.ihub.notification.event.AuctionEndedEvent;
import com.ihub.notification.event.AuctionStartedEvent;
import com.ihub.notification.event.WinnerAnnouncedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    public AuctionLifecycleService(
            AuctionDao auctionDao,
            AuctionLifecycleDao lifecycleDao,
            AuctionEventDao eventDao,
            IdeaDao ideaDao,
            BidDao bidDao,
            IdeaSearchService ideaSearchService,
            ApplicationEventPublisher eventPublisher) {
        this.auctionDao = auctionDao;
        this.lifecycleDao = lifecycleDao;
        this.eventDao = eventDao;
        this.ideaDao = ideaDao;
        this.bidDao = bidDao;
        this.ideaSearchService = ideaSearchService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void processDueAuctions() {
        List<Auction> toStart = lifecycleDao.findDueToStart();
        for (Auction auction : toStart) {
            startAuctionInternal(auction, "STARTED", "Auction auto-started at scheduled time");
        }

        List<Auction> toClose = lifecycleDao.findDueToClose();
        for (Auction auction : toClose) {
            closeAuctionInternal(auction, "CLOSED", "Auction auto-closed at scheduled end time");
        }

        if (!toStart.isEmpty() || !toClose.isEmpty()) {
            log.info("Auction lifecycle processed: started={}, closed={}", toStart.size(), toClose.size());
        }
    }

    @Transactional
    public Auction startAuction(Long auctionId, boolean manual) {
        Auction auction = getAuctionOrThrow(auctionId);

        if (!"SCHEDULED".equalsIgnoreCase(auction.getStatus())) {
            throw new CustomException("Only scheduled auctions can be started");
        }

        String eventType = manual ? "MANUAL_START" : "STARTED";
        String details = manual ? "Auction manually started by creator" : "Auction started";
        startAuctionInternal(auction, eventType, details);

        return getAuctionOrThrow(auctionId);
    }

    @Transactional
    public Auction closeAuction(Long auctionId, boolean manual) {
        Auction auction = getAuctionOrThrow(auctionId);

        if (!"ACTIVE".equalsIgnoreCase(auction.getStatus())) {
            throw new CustomException("Only active auctions can be closed");
        }

        String eventType = manual ? "MANUAL_CLOSE" : "CLOSED";
        String details = manual ? "Auction manually closed" : "Auction closed";
        closeAuctionInternal(auction, eventType, details);

        return getAuctionOrThrow(auctionId);
    }

    public void recordScheduledEvent(Long auctionId) {
        eventDao.recordEvent(auctionId, "SCHEDULED", "Auction created and scheduled");
    }

    private void startAuctionInternal(Auction auction, String eventType, String details) {
        int updated = lifecycleDao.activateById(auction.getId());
        if (updated == 0) {
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
        Optional<WinnerInfo> winner = selectAndSaveWinner(auction.getId());

        int updated = lifecycleDao.closeById(auction.getId());
        if (updated == 0) {
            return;
        }

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
            throw new CustomException("Auction not found");
        }
    }

    private record WinnerInfo(Long investorId, Double amount) {
    }
}
