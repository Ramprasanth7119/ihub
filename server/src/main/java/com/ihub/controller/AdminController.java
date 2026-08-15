package com.ihub.controller;

import com.ihub.dto.*;
import com.ihub.service.AdminService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin console API. Every route is gated on {@code ROLE_ADMIN} by
 * {@code SecurityConfig}, and re-checked against the live user record in
 * {@code AdminService.assertAdmin()}.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    // ---------------------------------------------------------------- Dashboard

    @GetMapping("/dashboard")
    public AdminDashboardResponse getDashboard() {
        return adminService.getDashboard();
    }

    @GetMapping("/metrics")
    public PlatformMetricsResponse getMetrics() {
        return adminService.getMetrics();
    }

    @GetMapping("/dashboard/charts")
    public DashboardChartData getDashboardCharts() {
        return adminService.getDashboardChartData();
    }

    // ---------------------------------------------------------- User management

    @GetMapping("/users")
    public AdminPageResponse<AdminUserResponse> getUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false) Integer size) {
        return adminService.getUsers(role, active, page, size);
    }

    @PatchMapping("/users/{id}/status")
    public AdminUserResponse updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UserStatusUpdateRequest request) {
        return adminService.updateUserStatus(id, request);
    }

    // ---------------------------------------------------------- Idea management

    @GetMapping("/ideas")
    public AdminPageResponse<AdminIdeaResponse> getIdeas(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false) Integer size) {
        return adminService.getIdeas(status, category, page, size);
    }

    @PatchMapping("/ideas/{id}/status")
    public AdminIdeaResponse updateIdeaStatus(
            @PathVariable Long id,
            @Valid @RequestBody IdeaStatusUpdateRequest request,
            HttpServletRequest httpRequest) {
        return adminService.updateIdeaStatus(id, request, httpRequest);
    }

    @DeleteMapping("/ideas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteIdea(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        adminService.deleteIdea(id, httpRequest);
    }

    // ------------------------------------------------------- Auction management

    @GetMapping("/auctions")
    public AdminPageResponse<AdminAuctionSummaryResponse> getAuctions(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false) Integer size) {
        return adminService.getAuctions(status, search, page, size);
    }

    @GetMapping("/auctions/{id}")
    public AdminAuctionSummaryResponse getAuction(@PathVariable Long id) {
        return adminService.getAuction(id);
    }

    @PostMapping("/auctions")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminAuctionSummaryResponse createAuction(
            @Valid @RequestBody CreateAuctionRequest request,
            HttpServletRequest httpRequest) {
        return adminService.createAuction(request, httpRequest);
    }

    @PatchMapping("/auctions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateAuction(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAuctionRequest request,
            HttpServletRequest httpRequest) {
        adminService.updateAuction(id, request, httpRequest);
    }

    @PostMapping("/auctions/{id}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelAuction(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        adminService.cancelAuction(id, httpRequest);
    }

    @PostMapping("/auctions/{id}/start")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void startAuction(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        adminService.startAuction(id, httpRequest);
    }

    @PostMapping("/auctions/{id}/end")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void endAuction(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        adminService.endAuction(id, httpRequest);
    }

    /** Decided auctions joined with idea, creator and winner in a single query. */
    @GetMapping("/winners")
    public AdminPageResponse<AuctionWinnerSummaryResponse> getWinners(
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false) Integer size) {
        return adminService.getWinners(page, size);
    }

    // ------------------------------------------------------------------- Bids

    @GetMapping("/bids")
    public AdminPageResponse<AdminBidResponse> getBids(
            @RequestParam(required = false) Long auctionId,
            @RequestParam(required = false) Long investorId,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false) Integer size) {
        return adminService.getBids(auctionId, investorId, page, size);
    }

    // ------------------------------------------------------------- Audit logs

    @GetMapping("/audit-logs")
    public AdminPageResponse<AdminAuditLogResponse> getAuditLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false) Integer size) {
        return adminService.getAuditLogs(action, entityType, page, size);
    }

    // ---------------------------------------------------------------- Settings

    @GetMapping("/settings")
    public PlatformSettingsResponse getSettings() {
        return adminService.getPlatformSettings();
    }

    @PutMapping("/settings")
    public PlatformSettingsResponse updateSettings(
            @Valid @RequestBody PlatformSettingsRequest request,
            HttpServletRequest httpRequest) {
        return adminService.updatePlatformSettings(request, httpRequest);
    }

    // ------------------------------------------------------------------ Search

    @GetMapping("/search/health")
    public Map<String, Object> getSearchHealth() {
        return adminService.getSearchHealth();
    }

    /** Rebuilds the Elasticsearch index from MySQL, repairing any drift. */
    @PostMapping("/search/reindex")
    public Map<String, Object> reindexSearch(HttpServletRequest httpRequest) {
        return adminService.reindexSearch(httpRequest);
    }
}
