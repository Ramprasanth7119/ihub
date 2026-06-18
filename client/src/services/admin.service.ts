import api from "@/lib/axios";
import type {
  AdminMetrics,
  AdminUser,
  AdminIdea,
  AdminAuction,
  AdminBid,
  AdminAuditLog,
  AdminPageResponse,
  DashboardChartData,
} from "@/types";

export const adminService = {
  // Dashboard
  getDashboard: () =>
    api
      .get<{ metrics: AdminMetrics; recentAuctions: AdminAuction[]; topCreators: any[]; topInvestors: any[] }>(
        "/admin/dashboard"
      )
      .then((r) => r.data),

  getMetrics: () => api.get<AdminMetrics>("/admin/metrics").then((r) => r.data),

  getDashboardCharts: () =>
    api.get<DashboardChartData>("/admin/dashboard/charts").then((r) => r.data),

  // User Management
  getUsers: (params?: { role?: string; active?: boolean; page?: number; size?: number }) =>
    api
      .get<AdminPageResponse<AdminUser>>("/admin/users", { params })
      .then((r) => r.data),

  updateUserStatus: (id: number, active: boolean) =>
    api.patch<AdminUser>(`/admin/users/${id}/status`, { active }).then((r) => r.data),

  // Idea Management
  getIdeas: (params?: { status?: string; category?: string; page?: number; size?: number }) =>
    api.get<AdminPageResponse<AdminIdea>>("/admin/ideas", { params }).then((r) => r.data),

  updateIdeaStatus: (id: number, status: string, reason?: string) =>
    api
      .patch<AdminIdea>(`/admin/ideas/${id}/status`, { status, reason })
      .then((r) => r.data),

  deleteIdea: (id: number) => api.delete(`/admin/ideas/${id}`),

  // Auction Management
  getAuctions: (params?: { status?: string; page?: number; size?: number }) =>
    api.get<AdminPageResponse<AdminAuction>>("/admin/auctions", { params }).then((r) => r.data),

  createAuction: (data: {
    ideaId: number;
    startTime: string;
    endTime: string;
    minBid?: number;
    reservePrice?: number;
    description?: string;
  }) => api.post<AdminAuction>("/admin/auctions", data).then((r) => r.data),

  updateAuction: (id: number, data: {
    startTime?: string;
    endTime?: string;
    minBidIncrement?: number;
  }) => api.patch(`/admin/auctions/${id}`, data),

  cancelAuction: (id: number) => api.post(`/admin/auctions/${id}/cancel`),

  startAuction: (id: number) => api.post(`/admin/auctions/${id}/start`),

  endAuction: (id: number) => api.post(`/admin/auctions/${id}/end`),

  // Bid Management
  getBids: (params?: { auctionId?: number; investorId?: number; page?: number; size?: number }) =>
    api.get<AdminPageResponse<AdminBid>>("/admin/bids", { params }).then((r) => r.data),

  // Audit Logs
  getAuditLogs: (params?: { action?: string; entityType?: string; page?: number; size?: number }) =>
    api.get<AdminPageResponse<AdminAuditLog>>("/admin/audit-logs", { params }).then((r) => r.data),
};
