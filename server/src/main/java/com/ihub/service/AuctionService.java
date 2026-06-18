package com.ihub.service;

import com.ihub.dao.AuctionDao;
import com.ihub.dao.AuctionEventDao;
import com.ihub.dao.UserDao;
import com.ihub.dto.AuctionHistoryResponse;
import com.ihub.dto.AuctionRequest;
import com.ihub.dto.AuctionResponse;
import com.ihub.dto.AuctionWinnerResponse;
import com.ihub.exception.CustomException;
import com.ihub.model.Auction;
import com.ihub.model.AuctionEvent;
import com.ihub.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AuctionService {

    private final AuctionDao auctionDao;
    private final AuctionEventDao eventDao;
    private final AuctionLifecycleService lifecycleService;
    private final IdeaSearchService ideaSearchService;
    private final UserDao userDao;
    private final double defaultMinBidIncrement;

    public AuctionService(
            AuctionDao auctionDao,
            AuctionEventDao eventDao,
            AuctionLifecycleService lifecycleService,
            IdeaSearchService ideaSearchService,
            UserDao userDao,
            @Value("${auction.default-min-bid-increment:100}") double defaultMinBidIncrement) {
        this.auctionDao = auctionDao;
        this.eventDao = eventDao;
        this.lifecycleService = lifecycleService;
        this.ideaSearchService = ideaSearchService;
        this.userDao = userDao;
        this.defaultMinBidIncrement = defaultMinBidIncrement;
    }

    @Transactional
    public AuctionResponse createAuction(AuctionRequest request) {
        validateTimes(request.getStartTime(), request.getEndTime());

        if (!auctionDao.ideaExists(request.getIdeaId())) {
            throw new CustomException("Idea not found");
        }

        if (!auctionDao.isIdeaPublished(request.getIdeaId())) {
            throw new CustomException("Only published ideas can be auctioned");
        }

        assertCreatorOwnsIdea(request.getIdeaId());

        if (auctionDao.auctionExistsForIdea(request.getIdeaId())) {
            throw new CustomException("An active or scheduled auction already exists for this idea");
        }

        Long id = auctionDao.createAuction(request, defaultMinBidIncrement);
        lifecycleService.recordScheduledEvent(id);
        ideaSearchService.updateStatus(request.getIdeaId(), "SCHEDULED");

        return map(auctionDao.getAuctionById(id));
    }

    public AuctionResponse getAuction(Long id) {
        return map(getAuctionOrThrow(id));
    }

    public List<AuctionResponse> getAuctions(String status) {
        return auctionDao.findAuctions(status)
                .stream()
                .map(this::map)
                .collect(Collectors.toList());
    }

    public AuctionWinnerResponse getWinner(Long auctionId) {
        getAuctionOrThrow(auctionId);

        Map<String, Object> row = auctionDao.findWinnerByAuctionId(auctionId);
        if (row == null) {
            throw new CustomException("Winner not yet determined for this auction");
        }

        return new AuctionWinnerResponse(
                ((Number) row.get("auction_id")).longValue(),
                ((Number) row.get("winner_id")).longValue(),
                (String) row.get("winner_name"),
                ((Number) row.get("winning_bid")).doubleValue(),
                toLocalDateTime(row.get("created_at"))
        );
    }

    public List<AuctionHistoryResponse> getHistory(Long auctionId) {
        getAuctionOrThrow(auctionId);

        return eventDao.findByAuctionId(auctionId)
                .stream()
                .map(this::toHistory)
                .collect(Collectors.toList());
    }

    @Transactional
    public AuctionResponse startAuction(Long id) {
        assertCreatorOwnsAuction(id);
        Auction auction = lifecycleService.startAuction(id, true);
        return map(auction);
    }

    @Transactional
    public AuctionResponse closeAuction(Long id) {
        assertCanCloseAuction(id);
        Auction auction = lifecycleService.closeAuction(id, true);
        return map(auction);
    }

    private Auction getAuctionOrThrow(Long id) {
        try {
            return auctionDao.getAuctionById(id);
        } catch (EmptyResultDataAccessException e) {
            throw new CustomException("Auction not found");
        }
    }

    private void validateTimes(LocalDateTime start, LocalDateTime end) {
        if (!end.isAfter(start)) {
            throw new CustomException("End time must be after start time");
        }
    }

    private void assertCreatorOwnsIdea(Long ideaId) {
        User user = getAuthenticatedUser();
        if (!"CREATOR".equalsIgnoreCase(user.getRole())) {
            throw new CustomException("Only creators can manage auctions");
        }

        Long creatorId = auctionDao.getIdeaCreatorId(ideaId);
        if (!creatorId.equals(user.getId())) {
            throw new CustomException("You can only create auctions for your own ideas");
        }
    }

    private void assertCreatorOwnsAuction(Long auctionId) {
        Auction auction = getAuctionOrThrow(auctionId);
        assertCreatorOwnsIdea(auction.getIdeaId());
    }

    private void assertCanCloseAuction(Long auctionId) {
        User user = getAuthenticatedUser();
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            return;
        }
        assertCreatorOwnsAuction(auctionId);
    }

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new CustomException("Authentication required");
        }

        User user = userDao.findByEmail(auth.getName());
        if (user == null) {
            throw new CustomException("User not found");
        }
        return user;
    }

    private AuctionResponse map(Auction auction) {
        return new AuctionResponse(
                auction.getId(),
                auction.getIdeaId(),
                auction.getStartTime(),
                auction.getEndTime(),
                auction.getMinBidIncrement(),
                auction.getStatus()
        );
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        if (value instanceof LocalDateTime dateTime) {
            return dateTime;
        }
        return null;
    }

    private AuctionHistoryResponse toHistory(AuctionEvent event) {
        return new AuctionHistoryResponse(
                event.getId(),
                event.getEventType(),
                event.getDetails(),
                event.getCreatedAt()
        );
    }
}
