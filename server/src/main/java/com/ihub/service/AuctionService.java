package com.ihub.service;

import com.ihub.dao.AuctionDao;
import com.ihub.dao.AuctionEventDao;
import com.ihub.dao.UserDao;
import com.ihub.dto.AuctionHistoryResponse;
import com.ihub.dto.AuctionRequest;
import com.ihub.dto.AuctionResponse;
import com.ihub.dto.AuctionWinnerResponse;
import com.ihub.exception.ConflictException;
import com.ihub.exception.ForbiddenException;
import com.ihub.exception.NotFoundException;
import com.ihub.exception.UnauthorizedException;
import com.ihub.exception.UnprocessableEntityException;
import com.ihub.model.Auction;
import com.ihub.model.AuctionEvent;
import com.ihub.model.PagedResult;
import com.ihub.model.User;
import com.ihub.util.Pagination;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class AuctionService {

    private final AuctionDao auctionDao;
    private final AuctionEventDao eventDao;
    private final AuctionLifecycleService lifecycleService;
    private final IdeaSearchService ideaSearchService;
    private final UserDao userDao;
    private final PlatformSettingsService settingsService;

    public AuctionService(
            AuctionDao auctionDao,
            AuctionEventDao eventDao,
            AuctionLifecycleService lifecycleService,
            IdeaSearchService ideaSearchService,
            UserDao userDao,
            PlatformSettingsService settingsService) {
        this.auctionDao = auctionDao;
        this.eventDao = eventDao;
        this.lifecycleService = lifecycleService;
        this.ideaSearchService = ideaSearchService;
        this.userDao = userDao;
        this.settingsService = settingsService;
    }

    @Transactional
    public AuctionResponse createAuction(AuctionRequest request) {
        validateTimes(request.getStartTime(), request.getEndTime());

        if (!auctionDao.ideaExists(request.getIdeaId())) {
            throw new NotFoundException("Idea not found");
        }

        if (!auctionDao.isIdeaPublished(request.getIdeaId())) {
            throw new ConflictException("Only published ideas can be auctioned");
        }

        assertCreatorOwnsIdea(request.getIdeaId());

        if (auctionDao.auctionExistsForIdea(request.getIdeaId())) {
            throw new ConflictException("An active or scheduled auction already exists for this idea");
        }

        // The platform default is admin-configurable and overrides the static
        // application.yaml value once settings have been saved.
        Long id = auctionDao.createAuction(request, settingsService.getDefaultBidIncrement());
        lifecycleService.recordScheduledEvent(id);
        ideaSearchService.updateStatus(request.getIdeaId(), "SCHEDULED");

        return map(auctionDao.getAuctionById(id));
    }

    public AuctionResponse getAuction(Long id) {
        return map(getAuctionOrThrow(id));
    }

    public PagedResult<AuctionResponse> getAuctions(String status, Integer page, Integer size) {
        int resolvedPage = Pagination.resolvePage(page);
        int resolvedSize = Pagination.resolveSize(size, 50, Pagination.MAX_PAGE_SIZE);

        List<AuctionResponse> content = auctionDao
                .findAuctions(status, resolvedSize, Pagination.offset(resolvedPage, resolvedSize))
                .stream()
                .map(this::map)
                .toList();

        return new PagedResult<>(content, auctionDao.countAuctions(status), resolvedPage, resolvedSize);
    }

    public AuctionWinnerResponse getWinner(Long auctionId) {
        getAuctionOrThrow(auctionId);

        Map<String, Object> row = auctionDao.findWinnerByAuctionId(auctionId);
        if (row == null) {
            throw new NotFoundException("Winner not yet determined for this auction");
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
                .toList();
    }

    @Transactional
    public AuctionResponse startAuction(Long id) {
        assertCanManageAuction(id);
        return map(lifecycleService.startAuction(id, true));
    }

    @Transactional
    public AuctionResponse closeAuction(Long id) {
        assertCanManageAuction(id);
        return map(lifecycleService.closeAuction(id, true));
    }

    private Auction getAuctionOrThrow(Long id) {
        try {
            return auctionDao.getAuctionById(id);
        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException("Auction not found");
        }
    }

    private void validateTimes(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new UnprocessableEntityException("Start and end time are required");
        }
        if (!end.isAfter(start)) {
            throw new UnprocessableEntityException("End time must be after start time");
        }
    }

    private void assertCreatorOwnsIdea(Long ideaId) {
        User user = getAuthenticatedUser();
        if (!"CREATOR".equalsIgnoreCase(user.getRole())) {
            throw new ForbiddenException("Only creators can manage auctions");
        }

        Long creatorId = auctionDao.getIdeaCreatorId(ideaId);
        if (creatorId == null || !creatorId.equals(user.getId())) {
            throw new ForbiddenException("You can only create auctions for your own ideas");
        }
    }

    /**
     * Admins may drive any auction; creators only their own. Resolved before the
     * transition so an unauthorised caller never mutates state.
     */
    private void assertCanManageAuction(Long auctionId) {
        User user = getAuthenticatedUser();
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            return;
        }

        Auction auction = getAuctionOrThrow(auctionId);
        assertCreatorOwnsIdea(auction.getIdeaId());
    }

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedException("Authentication required");
        }

        User user = userDao.findByEmail(auth.getName());
        if (user == null) {
            throw new UnauthorizedException("Authenticated user no longer exists");
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
