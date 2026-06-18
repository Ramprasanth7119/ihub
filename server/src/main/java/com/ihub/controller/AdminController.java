package com.ihub.controller;

import com.ihub.dto.*;
import com.ihub.service.AdminService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    // Dashboard
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

    // User Management
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

    // Idea Management
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
    public void deleteIdea(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        adminService.deleteIdea(id, httpRequest);
    }

    // Auction Management
    @GetMapping("/auctions")
    public AdminPageResponse<AdminAuctionSummaryResponse> getAuctions(
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false) Integer size) {
        return adminService.getAuctions(status, page, size);
    }

    @PostMapping("/auctions")
    public AdminAuctionSummaryResponse createAuction(
            @Valid @RequestBody CreateAuctionRequest request,
            HttpServletRequest httpRequest) {
        return adminService.createAuction(request, httpRequest);
    }

    @PatchMapping("/auctions/{id}")
    public void updateAuction(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAuctionRequest request,
            HttpServletRequest httpRequest) {
        adminService.updateAuction(id, request, httpRequest);
    }

    @PostMapping("/auctions/{id}/cancel")
    public void cancelAuction(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        adminService.cancelAuction(id, httpRequest);
    }

    @PostMapping("/auctions/{id}/start")
    public void startAuction(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        adminService.startAuction(id, httpRequest);
    }

    @PostMapping("/auctions/{id}/end")
    public void endAuction(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        adminService.endAuction(id, httpRequest);
    }

    // Bid Management
    @GetMapping("/bids")
    public AdminPageResponse<AdminBidResponse> getBids(
            @RequestParam(required = false) Long auctionId,
            @RequestParam(required = false) Long investorId,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false) Integer size) {
        return adminService.getBids(auctionId, investorId, page, size);
    }

    // Audit Logs
    @GetMapping("/audit-logs")
    public AdminPageResponse<AdminAuditLogResponse> getAuditLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false) Integer size) {
        return adminService.getAuditLogs(action, entityType, page, size);
    }
}
