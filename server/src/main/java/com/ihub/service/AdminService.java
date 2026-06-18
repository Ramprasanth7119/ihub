package com.ihub.service;

import com.ihub.dao.AdminDao;
import com.ihub.dao.AuctionDao;
import com.ihub.dao.IdeaDao;
import com.ihub.dao.UserDao;
import com.ihub.dto.*;
import com.ihub.exception.CustomException;
import com.ihub.model.Auction;
import com.ihub.model.Idea;
import com.ihub.model.User;
import com.ihub.dto.AuctionRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AdminService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int DASHBOARD_RECENT_LIMIT = 5;
    private static final int DASHBOARD_TOP_LIMIT = 5;

    private final AdminDao adminDao;
    private final UserDao userDao;
    private final IdeaDao ideaDao;
    private final AuctionDao auctionDao;
    private final double defaultMinBidIncrement;

    public AdminService(
            AdminDao adminDao,
            UserDao userDao,
            IdeaDao ideaDao,
            AuctionDao auctionDao,
            @Value("${auction.default-min-bid-increment:100}") double defaultMinBidIncrement) {
        this.adminDao = adminDao;
        this.userDao = userDao;
        this.ideaDao = ideaDao;
        this.auctionDao = auctionDao;
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

    public AdminPageResponse<AdminAuctionSummaryResponse> getAuctions(String status, Integer page, Integer size) {
        assertAdmin();
        int resolvedPage = page != null && page >= 0 ? page : 0;
        int resolvedSize = resolvePageSize(size);
        int offset = resolvedPage * resolvedSize;

        return new AdminPageResponse<>(
                adminDao.findAuctionsForAdmin(status, resolvedSize, offset),
                adminDao.countAuctionsForAdmin(status),
                resolvedPage,
                resolvedSize
        );
    }

    @Transactional
    public AdminUserResponse updateUserStatus(Long userId, UserStatusUpdateRequest request) {
        User admin = assertAdmin();

        if (admin.getId().equals(userId)) {
            throw new CustomException("You cannot change your own account status");
        }

        try {
            userDao.getUserById(userId);
        } catch (EmptyResultDataAccessException e) {
            throw new CustomException("User not found");
        }

        int rowsUpdated = adminDao.updateUserActive(userId, request.getActive());
        if (rowsUpdated == 0) {
            throw new CustomException("User not found");
        }

        AdminUserResponse userResponse = adminDao.findUserByIdForAdmin(userId);
        if (userResponse == null) {
            throw new CustomException("User not found");
        }
        return userResponse;
    }

    private User assertAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new CustomException("Authentication required");
        }

        User user = userDao.findByEmail(auth.getName());
        if (user == null) {
            throw new CustomException("User not found");
        }
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new CustomException("Admin access required");
        }
        if (!adminDao.isUserActive(user.getId())) {
            throw new CustomException("Account is suspended");
        }
        return user;
    }

    private int resolvePageSize(Integer size) {
        if (size == null || size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        if (size > MAX_PAGE_SIZE) {
            throw new CustomException("Page size cannot exceed " + MAX_PAGE_SIZE);
        }
        return size;
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
        
        try {
            Idea idea = ideaDao.getIdeaById(ideaId);
            if (idea == null) {
                throw new CustomException("Idea not found");
            }
        } catch (EmptyResultDataAccessException e) {
            throw new CustomException("Idea not found");
        }

        int rowsUpdated = adminDao.updateIdeaStatus(ideaId, request.getStatus());
        if (rowsUpdated == 0) {
            throw new CustomException("Failed to update idea status");
        }

        // Log the action
        String ipAddress = getClientIp(httpRequest);
        adminDao.createAuditLog(admin.getId(), "IDEA_STATUS_UPDATE", "IDEA", ideaId, 
                "Status changed to " + request.getStatus() + (request.getReason() != null ? ". Reason: " + request.getReason() : ""), 
                ipAddress);

        // Send notification to creator
        // This would integrate with NotificationService

        AdminIdeaResponse updatedIdea = adminDao.findIdeaByIdForAdmin(ideaId);
        if (updatedIdea == null) {
            throw new CustomException("Idea not found");
        }
        return updatedIdea;
    }

    @Transactional
    public void deleteIdea(Long ideaId, HttpServletRequest httpRequest) {
        User admin = assertAdmin();
        
        try {
            Idea idea = ideaDao.getIdeaById(ideaId);
            if (idea == null) {
                throw new CustomException("Idea not found");
            }
        } catch (EmptyResultDataAccessException e) {
            throw new CustomException("Idea not found");
        }

        int rowsDeleted = adminDao.deleteIdea(ideaId);
        if (rowsDeleted == 0) {
            throw new CustomException("Failed to delete idea");
        }

        String ipAddress = getClientIp(httpRequest);
        adminDao.createAuditLog(admin.getId(), "IDEA_DELETE", "IDEA", ideaId, "Idea deleted", ipAddress);
    }

    // Auction Management
    @Transactional
    public AdminAuctionSummaryResponse createAuction(CreateAuctionRequest request, HttpServletRequest httpRequest) {
        User admin = assertAdmin();

        // Validate idea exists and is approved
        try {
            Idea idea = ideaDao.getIdeaById(request.getIdeaId());
            if (idea == null) {
                throw new CustomException("Idea not found");
            }
            if (!"APPROVED".equalsIgnoreCase(idea.getStatus()) && !"PUBLISHED".equalsIgnoreCase(idea.getStatus())) {
                throw new CustomException("Idea must be APPROVED or PUBLISHED to create an auction");
            }
        } catch (EmptyResultDataAccessException e) {
            throw new CustomException("Idea not found");
        }

        // Validate end time > start time
        if (request.getEndTime().isBefore(request.getStartTime()) || request.getEndTime().isEqual(request.getStartTime())) {
            throw new CustomException("End time must be after start time");
        }

        // Check for duplicate active auction
        if (adminDao.hasActiveAuctionForIdea(request.getIdeaId())) {
            throw new CustomException("An active auction already exists for this idea");
        }

        // Create auction
        AuctionRequest auctionRequest = new AuctionRequest();
        auctionRequest.setIdeaId(request.getIdeaId());
        auctionRequest.setStartTime(request.getStartTime());
        auctionRequest.setEndTime(request.getEndTime());
        auctionRequest.setMinBidIncrement(request.getMinBid());

        Long auctionId = auctionDao.createAuction(auctionRequest, defaultMinBidIncrement);
        if (auctionId == null) {
            throw new CustomException("Failed to create auction");
        }

        // Save auction settings
        adminDao.saveAuctionSettings(request.getIdeaId(), request.getMinBid(), request.getReservePrice(), request.getDescription());

        // Log the action
        String ipAddress = getClientIp(httpRequest);
        adminDao.createAuditLog(admin.getId(), "AUCTION_CREATE", "AUCTION", auctionId, 
                "Auction created for idea " + request.getIdeaId(), ipAddress);

        AdminAuctionSummaryResponse createdAuction = adminDao.findAuctionByIdForAdmin(auctionId);
        if (createdAuction == null) {
            throw new CustomException("Failed to load created auction");
        }
        return createdAuction;
    }

    @Transactional
    public void updateAuction(Long auctionId, UpdateAuctionRequest request, HttpServletRequest httpRequest) {
        User admin = assertAdmin();

        if (request.getStartTime() == null && request.getEndTime() == null && request.getMinBidIncrement() == null) {
            throw new CustomException("At least one auction field must be provided");
        }

        try {
            Auction auction = auctionDao.getAuctionById(auctionId);
            if (auction == null) {
                throw new CustomException("Auction not found");
            }
            if ("ACTIVE".equalsIgnoreCase(auction.getStatus()) || "CLOSED".equalsIgnoreCase(auction.getStatus())) {
                throw new CustomException("Cannot update active or closed auctions");
            }
        } catch (EmptyResultDataAccessException e) {
            throw new CustomException("Auction not found");
        }

        // Validate end time > start time if both provided
        if (request.getStartTime() != null && request.getEndTime() != null) {
            if (request.getEndTime().isBefore(request.getStartTime()) || request.getEndTime().isEqual(request.getStartTime())) {
                throw new CustomException("End time must be after start time");
            }
        }

        int rowsUpdated = adminDao.updateAuctionDetails(auctionId, request.getStartTime(), request.getEndTime(), request.getMinBidIncrement());
        if (rowsUpdated == 0) {
            throw new CustomException("Failed to update auction");
        }

        String ipAddress = getClientIp(httpRequest);
        adminDao.createAuditLog(admin.getId(), "AUCTION_UPDATE", "AUCTION", auctionId, "Auction details updated", ipAddress);
    }

    @Transactional
    public void cancelAuction(Long auctionId, HttpServletRequest httpRequest) {
        User admin = assertAdmin();

        try {
            Auction auction = auctionDao.getAuctionById(auctionId);
            if (auction == null) {
                throw new CustomException("Auction not found");
            }
            if ("CLOSED".equalsIgnoreCase(auction.getStatus()) || "CANCELLED".equalsIgnoreCase(auction.getStatus())) {
                throw new CustomException("Auction is already closed or cancelled");
            }
        } catch (EmptyResultDataAccessException e) {
            throw new CustomException("Auction not found");
        }

        int rowsUpdated = adminDao.updateAuctionStatus(auctionId, "CANCELLED");
        if (rowsUpdated == 0) {
            throw new CustomException("Failed to cancel auction");
        }

        String ipAddress = getClientIp(httpRequest);
        adminDao.createAuditLog(admin.getId(), "AUCTION_CANCEL", "AUCTION", auctionId, "Auction cancelled", ipAddress);
    }

    @Transactional
    public void startAuction(Long auctionId, HttpServletRequest httpRequest) {
        User admin = assertAdmin();

        try {
            Auction auction = auctionDao.getAuctionById(auctionId);
            if (auction == null) {
                throw new CustomException("Auction not found");
            }
            if (!"UPCOMING".equalsIgnoreCase(auction.getStatus()) && !"SCHEDULED".equalsIgnoreCase(auction.getStatus())) {
                throw new CustomException("Auction must be UPCOMING or SCHEDULED to start");
            }
        } catch (EmptyResultDataAccessException e) {
            throw new CustomException("Auction not found");
        }

        int rowsUpdated = adminDao.updateAuctionStatus(auctionId, "ACTIVE");
        if (rowsUpdated == 0) {
            throw new CustomException("Failed to start auction");
        }

        String ipAddress = getClientIp(httpRequest);
        adminDao.createAuditLog(admin.getId(), "AUCTION_START", "AUCTION", auctionId, "Auction started manually", ipAddress);
    }

    @Transactional
    public void endAuction(Long auctionId, HttpServletRequest httpRequest) {
        User admin = assertAdmin();

        try {
            Auction auction = auctionDao.getAuctionById(auctionId);
            if (auction == null) {
                throw new CustomException("Auction not found");
            }
            if (!"ACTIVE".equalsIgnoreCase(auction.getStatus())) {
                throw new CustomException("Auction must be ACTIVE to end");
            }
        } catch (EmptyResultDataAccessException e) {
            throw new CustomException("Auction not found");
        }

        int rowsUpdated = adminDao.updateAuctionStatus(auctionId, "CLOSED");
        if (rowsUpdated == 0) {
            throw new CustomException("Failed to end auction");
        }

        String ipAddress = getClientIp(httpRequest);
        adminDao.createAuditLog(admin.getId(), "AUCTION_END", "AUCTION", auctionId, "Auction ended manually", ipAddress);
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
