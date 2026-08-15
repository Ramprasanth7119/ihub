import api from "@/lib/axios";
import type {
  AdminMetrics,
  AdminUser,
  AdminIdea,
  AdminAuction,
  AdminBid,
  AdminAuditLog,
  AdminPageResponse,
  AuctionWinnerSummary,
  CreateAuctionPayload,
  DashboardChartData,
  PlatformSettings,
  ReindexResult,
  SearchHealth,
  UpdateAuctionPayload,
} from "@/types";

/** A creator or investor ranked on the admin dashboard leaderboards. */
export interface LeaderStat {
  userId: number;
  name: string;
  email: string;
  statCount: number;
}

export interface AdminDashboard {
  metrics: AdminMetrics;
  recentAuctions: AdminAuction[];
  topCreators: LeaderStat[];
  topInvestors: LeaderStat[];
}

export const adminService = {
  // ------------------------------------------------------------- Dashboard
  getDashboard: () => api.get<AdminDashboard>("/admin/dashboard").then((r) => r.data),

  getMetrics: () => api.get<AdminMetrics>("/admin/metrics").then((r) => r.data),

  getDashboardCharts: () =>
    api.get<DashboardChartData>("/admin/dashboard/charts").then((r) => r.data),

  // -------------------------------------------------------- User management
  getUsers: (params?: { role?: string; active?: boolean; page?: number; size?: number }) =>
    api.get<AdminPageResponse<AdminUser>>("/admin/users", { params }).then((r) => r.data),

  updateUserStatus: (id: number, active: boolean) =>
    api.patch<AdminUser>(`/admin/users/${id}/status`, { active }).then((r) => r.data),

  // -------------------------------------------------------- Idea management
  getIdeas: (params?: { status?: string; category?: string; page?: number; size?: number }) =>
    api.get<AdminPageResponse<AdminIdea>>("/admin/ideas", { params }).then((r) => r.data),

  updateIdeaStatus: (id: number, status: string, reason?: string) =>
    api.patch<AdminIdea>(`/admin/ideas/${id}/status`, { status, reason }).then((r) => r.data),

  deleteIdea: (id: number) => api.delete(`/admin/ideas/${id}`),

  // ----------------------------------------------------- Auction management
  getAuctions: (params?: { status?: string; search?: string; page?: number; size?: number }) =>
    api.get<AdminPageResponse<AdminAuction>>("/admin/auctions", { params }).then((r) => r.data),

  getAuction: (id: number) => api.get<AdminAuction>(`/admin/auctions/${id}`).then((r) => r.data),

  createAuction: (data: CreateAuctionPayload) =>
    api.post<AdminAuction>("/admin/auctions", data).then((r) => r.data),

  updateAuction: (id: number, data: UpdateAuctionPayload) =>
    api.patch(`/admin/auctions/${id}`, data),

  cancelAuction: (id: number) => api.post(`/admin/auctions/${id}/cancel`),

  startAuction: (id: number) => api.post(`/admin/auctions/${id}/start`),

  endAuction: (id: number) => api.post(`/admin/auctions/${id}/end`),

  /**
   * Every decided auction with its winner, resolved server-side in one query.
   * Replaces fetching the auction list and then one winner lookup per row.
   */
  getWinners: (params?: { page?: number; size?: number }) =>
    api
      .get<AdminPageResponse<AuctionWinnerSummary>>("/admin/winners", { params })
      .then((r) => r.data),

  // ------------------------------------------------------------------ Bids
  getBids: (params?: { auctionId?: number; investorId?: number; page?: number; size?: number }) =>
    api.get<AdminPageResponse<AdminBid>>("/admin/bids", { params }).then((r) => r.data),

  // ------------------------------------------------------------ Audit logs
  getAuditLogs: (params?: { action?: string; entityType?: string; page?: number; size?: number }) =>
    api.get<AdminPageResponse<AdminAuditLog>>("/admin/audit-logs", { params }).then((r) => r.data),

  // -------------------------------------------------------------- Settings
  getSettings: () => api.get<PlatformSettings>("/admin/settings").then((r) => r.data),

  updateSettings: (settings: PlatformSettings) =>
    api.put<PlatformSettings>("/admin/settings", settings).then((r) => r.data),

  // ---------------------------------------------------------------- Search
  getSearchHealth: () => api.get<SearchHealth>("/admin/search/health").then((r) => r.data),

  /** Rebuilds the Elasticsearch index from MySQL, repairing drift after an outage. */
  reindexSearch: () => api.post<ReindexResult>("/admin/search/reindex").then((r) => r.data),
};
