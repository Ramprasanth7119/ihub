package com.ihub.service;

import com.ihub.dao.AdminDao;
import com.ihub.dao.AuctionDao;
import com.ihub.dao.IdeaDao;
import com.ihub.dao.RefreshTokenDao;
import com.ihub.dao.TagDao;
import com.ihub.dao.UserDao;
import com.ihub.dto.*;
import com.ihub.exception.ConflictException;
import com.ihub.exception.ForbiddenException;
import com.ihub.exception.NotFoundException;
import com.ihub.exception.UnauthorizedException;
import com.ihub.exception.UnprocessableEntityException;
import com.ihub.model.Auction;
import com.ihub.model.Idea;
import com.ihub.model.User;
import com.ihub.dto.AuctionRequest;
import com.ihub.search.IdeaDocument;
import com.ihub.util.Pagination;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int DASHBOARD_RECENT_LIMIT = 5;
    private static final int DASHBOARD_TOP_LIMIT = 5;

    /**
     * Upper bound on a single reindex pass. Well above any realistic catalogue size
     * for this platform, and it keeps one admin action from loading an unbounded
     * result set into memory.
     */
    private static final int REINDEX_BATCH_LIMIT = 10_000;

    private final AdminDao adminDao;
    private final UserDao userDao;
    private final IdeaDao ideaDao;
    private final AuctionDao auctionDao;
    private final TagDao tagDao;
    private final RefreshTokenDao refreshTokenDao;
    private final AuctionLifecycleService lifecycleService;
    private final IdeaSearchService ideaSearchService;
    private final NotificationService notificationService;
    private final PlatformSettingsService settingsService;
    private final double defaultMinBidIncrement;

    public AdminService(
            AdminDao adminDao,
            UserDao userDao,
            IdeaDao ideaDao,
            AuctionDao auctionDao,
            TagDao tagDao,
            RefreshTokenDao refreshTokenDao,
            AuctionLifecycleService lifecycleService,
            IdeaSearchService ideaSearchService,
            NotificationService notificationService,
            PlatformSettingsService settingsService,
            @Value("${auction.default-min-bid-increment:100}") double defaultMinBidIncrement) {
        this.settingsService = settingsService;
        this.adminDao = adminDao;
        this.userDao = userDao;
        this.ideaDao = ideaDao;
        this.auctionDao = auctionDao;
        this.tagDao = tagDao;
        this.refreshTokenDao = refreshTokenDao;
        this.lifecycleService = lifecycleService;
        this.ideaSearchService = ideaSearchService;
        this.notificationService = notificationService;
        this.defaultMinBidIncrement = defaultMinBidIncrement;
    }

    public AdminDashboardResponse getDashboard() {
        assertAdmin();

        AdminDashboardResponse dashboard = new AdminDashboardResponse();
        dashboard.setMetrics(adminDao.fetchPlatformMetrics());
        dashboard.setRecentAuctions(adminDao.findRecentAuctions(DASHBOARD_RECENT_LIMIT));
        dashboard.setTopCreators(adminDao.findTopCreators(DASHBOARD_TOP_LIMIT));
        dashboard.setTopInvestors(adminDao.findTopInvestors(DASHBOARD_TOP_LIMIT));
        return dashboard;
    }

    public PlatformMetricsResponse getMetrics() {
        assertAdmin();
        return adminDao.fetchPlatformMetrics();
    }

    public AdminPageResponse<AdminUserResponse> getUsers(String role, Boolean active, Integer page, Integer size) {
        assertAdmin();
        int resolvedPage = page != null && page >= 0 ? page : 0;
        int resolvedSize = resolvePageSize(size);
        int offset = resolvedPage * resolvedSize;

        return new AdminPageResponse<>(
                adminDao.findUsersForAdmin(role, active, resolvedSize, offset),
                adminDao.countUsersForAdmin(role, active),
                resolvedPage,
                resolvedSize
        );
    }

    public AdminPageResponse<AdminAuctionSummaryResponse> getAuctions(
            String status, String search, Integer page, Integer size) {
        assertAdmin();
        int resolvedPage = page != null && page >= 0 ? page : 0;
        int resolvedSize = resolvePageSize(size);
        int offset = resolvedPage * resolvedSize;

        return new AdminPageResponse<>(
                adminDao.findAuctionsForAdmin(status, search, resolvedSize, offset),
                adminDao.countAuctionsForAdmin(status, search),
                resolvedPage,
                resolvedSize
        );
    }

    @Transactional
    public AdminUserResponse updateUserStatus(Long userId, UserStatusUpdateRequest request) {
        User admin = assertAdmin();

        // Guards against an admin locking themselves out of the console.
        if (admin.getId().equals(userId)) {
            throw new ConflictException("You cannot change your own account status");
        }

        try {
            userDao.getUserById(userId);
        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException("User not found");
        }

        int rowsUpdated = adminDao.updateUserActive(userId, request.getActive());
        if (rowsUpdated == 0) {
            throw new NotFoundException("User not found");
        }

        // Deactivating a user must also end their sessions, otherwise an already
        // issued refresh token keeps minting access tokens until it expires.
        if (Boolean.FALSE.equals(request.getActive())) {
            refreshTokenDao.revokeAllForUser(userId);
        }

        AdminUserResponse userResponse = adminDao.findUserByIdForAdmin(userId);
        if (userResponse == null) {
            throw new NotFoundException("User not found");
        }
        return userResponse;
    }

    /**
     * Re-verifies admin rights against the database on every call.
     *
     * <p>The URL matchers in {@code SecurityConfig} already gate {@code /api/admin/**}
     * on the JWT role claim, but a token outlives a demotion or suspension, so the
     * live {@code users} row is authoritative here.</p>
     */
    private User assertAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedException("Authentication required");
        }

        User user = userDao.findByEmail(auth.getName());
        if (user == null) {
            throw new UnauthorizedException("Authenticated user no longer exists");
        }
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new ForbiddenException("Admin access required");
        }
        if (!adminDao.isUserActive(user.getId())) {
            throw new ForbiddenException("Your account is suspended");
        }
        return user;
    }

    private int resolvePageSize(Integer size) {
        return Pagination.resolveSize(size, DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE);
    }

    // Idea Management
    public AdminPageResponse<AdminIdeaResponse> getIdeas(String status, String category, Integer page, Integer size) {
        assertAdmin();
        int resolvedPage = page != null && page >= 0 ? page : 0;
        int resolvedSize = resolvePageSize(size);
        int offset = resolvedPage * resolvedSize;

        return new AdminPageResponse<>(
                adminDao.findIdeasForAdmin(status, category, resolvedSize, offset),
                adminDao.countIdeasForAdmin(status, category),
                resolvedPage,
                resolvedSize
        );
    }

    @Transactional
    public AdminIdeaResponse updateIdeaStatus(Long ideaId, IdeaStatusUpdateRequest request, HttpServletRequest httpRequest) {
        User admin = assertAdmin();
        Idea idea = getIdeaOrThrow(ideaId);

        int rowsUpdated = adminDao.updateIdeaStatus(ideaId, request.getStatus());
        if (rowsUpdated == 0) {
            throw new ConflictException("Idea status could not be updated");
        }

        audit(admin, httpRequest, "IDEA_STATUS_UPDATE", "IDEA", ideaId,
                "Status changed to " + request.getStatus()
                        + (request.getReason() != null ? ". Reason: " + request.getReason() : ""));

        // Keep the search index consistent with the moderation decision: a suspended
        // or rejected idea must stop appearing in investor discovery immediately.
        syncIdeaToSearchIndex(ideaId, request.getStatus());

        if (settingsService.isIdeaApprovalAlertEnabled()) {
            notificationService.notifyIdeaStatusChanged(
                    idea.getCreatorId(), ideaId, idea.getTitle(), request.getStatus(), request.getReason());
        }

        AdminIdeaResponse updatedIdea = adminDao.findIdeaByIdForAdmin(ideaId);
        if (updatedIdea == null) {
            throw new NotFoundException("Idea not found");
        }
        return updatedIdea;
    }

    @Transactional
    public void deleteIdea(Long ideaId, HttpServletRequest httpRequest) {
        User admin = assertAdmin();
        getIdeaOrThrow(ideaId);

        int rowsDeleted = adminDao.deleteIdea(ideaId);
        if (rowsDeleted == 0) {
            throw new ConflictException("Idea could not be deleted");
        }

        ideaSearchService.removeFromIndex(ideaId);
        audit(admin, httpRequest, "IDEA_DELETE", "IDEA", ideaId, "Idea deleted");
    }

    /**
     * Reflects a moderation decision in Elasticsearch. Only {@code PUBLISHED} and
     * {@code APPROVED} ideas belong in investor-facing search; everything else is
     * removed from the index while remaining intact in MySQL.
     */
    private void syncIdeaToSearchIndex(Long ideaId, String status) {
        boolean discoverable = "PUBLISHED".equalsIgnoreCase(status) || "APPROVED".equalsIgnoreCase(status);
        if (!discoverable) {
            ideaSearchService.removeFromIndex(ideaId);
            return;
        }

        Idea idea = ideaDao.getIdeaById(ideaId);
        IdeaDocument doc = new IdeaDocument();
        doc.setId(idea.getId());
        doc.setTitle(idea.getTitle());
        doc.setDescription(idea.getDescription());
        doc.setCategory(idea.getCategory());
        doc.setMinBudget(idea.getBasePrice());
        doc.setMaxBudget(idea.getMaxBudget());
        doc.setIdeaStatus("PUBLISHED");
        doc.setAuctionStatus(adminDao.hasActiveAuctionForIdea(ideaId) ? "SCHEDULED" : "NONE");
        doc.setTags(tagDao.findTagNamesByIdeaId(ideaId));
        ideaSearchService.indexIdea(doc);
    }

    private Idea getIdeaOrThrow(Long ideaId) {
        try {
            Idea idea = ideaDao.getIdeaById(ideaId);
            if (idea == null) {
                throw new NotFoundException("Idea not found");
            }
            return idea;
        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException("Idea not found");
        }
    }

    private void audit(User admin, HttpServletRequest httpRequest,
                       String action, String entityType, Long entityId, String details) {
        adminDao.createAuditLog(admin.getId(), action, entityType, entityId, details, getClientIp(httpRequest));
    }

    // Auction Management
    @Transactional
    public AdminAuctionSummaryResponse createAuction(CreateAuctionRequest request, HttpServletRequest httpRequest) {
        User admin = assertAdmin();

        Idea idea = getIdeaOrThrow(request.getIdeaId());
        if (!"APPROVED".equalsIgnoreCase(idea.getStatus()) && !"PUBLISHED".equalsIgnoreCase(idea.getStatus())) {
            throw new ConflictException("Idea must be APPROVED or PUBLISHED to create an auction");
        }

        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new UnprocessableEntityException("End time must be after start time");
        }

        if (auctionDao.auctionExistsForIdea(request.getIdeaId())) {
            throw new ConflictException("An active or scheduled auction already exists for this idea");
        }

        AuctionRequest auctionRequest = new AuctionRequest();
        auctionRequest.setIdeaId(request.getIdeaId());
        auctionRequest.setStartTime(request.getStartTime());
        auctionRequest.setEndTime(request.getEndTime());
        auctionRequest.setMinBidIncrement(request.getMinBidIncrement());

        Long auctionId = auctionDao.createAuction(auctionRequest, settingsService.getDefaultBidIncrement());

        adminDao.saveAuctionSettings(request.getIdeaId(), request.getMinBid(), request.getReservePrice(), request.getDescription());

        // Same bookkeeping the creator-facing path performs, so the auction shows up
        // in its own history feed and in investor search from the moment it exists.
        lifecycleService.recordScheduledEvent(auctionId);
        ideaSearchService.updateStatus(request.getIdeaId(), "SCHEDULED");

        audit(admin, httpRequest, "AUCTION_CREATE", "AUCTION", auctionId,
                "Auction created for idea " + request.getIdeaId());

        AdminAuctionSummaryResponse createdAuction = adminDao.findAuctionByIdForAdmin(auctionId);
        if (createdAuction == null) {
            throw new NotFoundException("Auction not found after creation");
        }
        return createdAuction;
    }

    @Transactional
    public void updateAuction(Long auctionId, UpdateAuctionRequest request, HttpServletRequest httpRequest) {
        User admin = assertAdmin();

        if (request.getStartTime() == null && request.getEndTime() == null && request.getMinBidIncrement() == null) {
            throw new UnprocessableEntityException("At least one auction field must be provided");
        }

        Auction auction;
        try {
            auction = auctionDao.getAuctionById(auctionId);
        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException("Auction not found");
        }
        if (auction == null) {
            throw new NotFoundException("Auction not found");
        }
        // Editing terms after bidding has opened would change the rules mid-auction.
        if (!"SCHEDULED".equalsIgnoreCase(auction.getStatus())) {
            throw new ConflictException(
                    "Only scheduled auctions can be edited (current status: " + auction.getStatus() + ")");
        }

        // Compare against the stored values for whichever side of the window was omitted.
        LocalDateTime start = request.getStartTime() != null ? request.getStartTime() : auction.getStartTime();
        LocalDateTime end = request.getEndTime() != null ? request.getEndTime() : auction.getEndTime();
        if (start != null && end != null && !end.isAfter(start)) {
            throw new UnprocessableEntityException("End time must be after start time");
        }

        int rowsUpdated = adminDao.updateAuctionDetails(
                auctionId, request.getStartTime(), request.getEndTime(), request.getMinBidIncrement());
        if (rowsUpdated == 0) {
            throw new ConflictException("Auction could not be updated");
        }

        audit(admin, httpRequest, "AUCTION_UPDATE", "AUCTION", auctionId, "Auction details updated");
    }

    /**
     * Cancels an auction.
     *
     * <p>Delegates to {@link AuctionLifecycleService} rather than writing the status
     * column directly. The lifecycle service owns the state-machine guards, the
     * {@code auction_events} history, Elasticsearch synchronisation and participant
     * notifications — bypassing it previously left admin-driven transitions silently
     * invisible to bidders and to the search index.</p>
     */
    @Transactional
    public void cancelAuction(Long auctionId, HttpServletRequest httpRequest) {
        User admin = assertAdmin();
        lifecycleService.cancelAuction(auctionId, "Cancelled by administrator");
        audit(admin, httpRequest, "AUCTION_CANCEL", "AUCTION", auctionId, "Auction cancelled");
    }

    @Transactional
    public void startAuction(Long auctionId, HttpServletRequest httpRequest) {
        User admin = assertAdmin();
        lifecycleService.startAuction(auctionId, true);
        audit(admin, httpRequest, "AUCTION_START", "AUCTION", auctionId, "Auction started manually");
    }

    /** Closes an auction, selecting and announcing the winner as a natural close would. */
    @Transactional
    public void endAuction(Long auctionId, HttpServletRequest httpRequest) {
        User admin = assertAdmin();
        lifecycleService.closeAuction(auctionId, true);
        audit(admin, httpRequest, "AUCTION_END", "AUCTION", auctionId, "Auction ended manually");
    }

    /** Full detail for one auction, including live bid statistics. */
    public AdminAuctionSummaryResponse getAuction(Long auctionId) {
        assertAdmin();
        AdminAuctionSummaryResponse auction = adminDao.findAuctionByIdForAdmin(auctionId);
        if (auction == null) {
            throw new NotFoundException("Auction not found");
        }
        return auction;
    }

    /** Every auction that has produced a winner, newest first. */
    public AdminPageResponse<AuctionWinnerSummaryResponse> getWinners(Integer page, Integer size) {
        assertAdmin();
        int resolvedPage = Pagination.resolvePage(page);
        int resolvedSize = Pagination.resolveSize(size);

        return new AdminPageResponse<>(
                adminDao.findWinners(resolvedSize, Pagination.offset(resolvedPage, resolvedSize)),
                adminDao.countWinners(),
                resolvedPage,
                resolvedSize
        );
    }

    // Bid Management
    public AdminPageResponse<AdminBidResponse> getBids(Long auctionId, Long investorId, Integer page, Integer size) {
        assertAdmin();
        int resolvedPage = page != null && page >= 0 ? page : 0;
        int resolvedSize = resolvePageSize(size);
        int offset = resolvedPage * resolvedSize;

        return new AdminPageResponse<>(
                adminDao.findBidsForAdmin(auctionId, investorId, resolvedSize, offset),
                adminDao.countBidsForAdmin(auctionId, investorId),
                resolvedPage,
                resolvedSize
        );
    }

    // Audit Logs
    public AdminPageResponse<AdminAuditLogResponse> getAuditLogs(String action, String entityType, Integer page, Integer size) {
        assertAdmin();
        int resolvedPage = page != null && page >= 0 ? page : 0;
        int resolvedSize = resolvePageSize(size);
        int offset = resolvedPage * resolvedSize;

        return new AdminPageResponse<>(
                adminDao.findAuditLogs(action, entityType, resolvedSize, offset),
                adminDao.countAuditLogs(action, entityType),
                resolvedPage,
                resolvedSize
        );
    }

    // Dashboard Charts
    public DashboardChartData getDashboardChartData() {
        assertAdmin();
        return adminDao.getDashboardChartData();
    }

    // Platform settings
    public PlatformSettingsResponse getPlatformSettings() {
        assertAdmin();
        return settingsService.getSettings();
    }

    @Transactional
    public PlatformSettingsResponse updatePlatformSettings(
            PlatformSettingsRequest request, HttpServletRequest httpRequest) {
        User admin = assertAdmin();
        PlatformSettingsResponse updated = settingsService.updateSettings(request, admin.getId());
        audit(admin, httpRequest, "SETTINGS_UPDATE", "PLATFORM", null, "Platform settings updated");
        return updated;
    }

    /**
     * Rebuilds the Elasticsearch index from MySQL.
     *
     * <p>Index writes elsewhere are best-effort and skipped on failure, so this is the
     * supported way to repair drift after an Elasticsearch outage.</p>
     */
    @Transactional
    public Map<String, Object> reindexSearch(HttpServletRequest httpRequest) {
        User admin = assertAdmin();

        List<Idea> published = ideaDao.findIdeas(
                "PUBLISHED", null, null, null, null, REINDEX_BATCH_LIMIT, 0);

        Map<Long, List<String>> tagsByIdea = tagDao.findTagNamesGroupedByIdeaIds(
                published.stream().map(Idea::getId).toList());
        Map<Long, String> auctionStatusByIdea = adminDao.findLatestAuctionStatusByIdea();

        List<IdeaDocument> documents = published.stream()
                .map(idea -> {
                    IdeaDocument doc = new IdeaDocument();
                    doc.setId(idea.getId());
                    doc.setTitle(idea.getTitle());
                    doc.setDescription(idea.getDescription());
                    doc.setCategory(idea.getCategory());
                    doc.setMinBudget(idea.getBasePrice());
                    doc.setMaxBudget(idea.getMaxBudget());
                    doc.setIdeaStatus(idea.getStatus());
                    doc.setAuctionStatus(auctionStatusByIdea.getOrDefault(idea.getId(), "NONE"));
                    doc.setTags(tagsByIdea.getOrDefault(idea.getId(), List.of()));
                    return doc;
                })
                .toList();

        int indexed = ideaSearchService.reindexAll(documents);
        audit(admin, httpRequest, "SEARCH_REINDEX", "SEARCH", null, "Reindexed " + indexed + " ideas");

        return Map.of("indexed", indexed, "searchAvailable", true);
    }

    /** Reports whether the search cluster is reachable, for the admin health panel. */
    public Map<String, Object> getSearchHealth() {
        assertAdmin();
        return Map.of("available", ideaSearchService.isSearchAvailable());
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
