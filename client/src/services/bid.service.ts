import api from "@/lib/axios";
import type {
  BidHistoryEntry,
  BidResponse,
  HighestBid,
  InvestorBid,
  LeaderboardEntry,
} from "@/types";

export const bidService = {
  placeBid: (auctionId: number, amount: number) =>
    api.post<BidResponse>("/bids", { auctionId, amount }).then((r) => r.data),

  getHistory: (auctionId: number, params?: { page?: number; size?: number }) =>
    api.get<BidHistoryEntry[]>(`/bids/auction/${auctionId}/history`, { params }).then((r) => r.data),

  getHighest: (auctionId: number) =>
    api.get<HighestBid>(`/bids/auction/${auctionId}/highest`).then((r) => r.data),

  getLeaderboard: (auctionId: number) =>
    api.get<LeaderboardEntry[]>(`/bids/auction/${auctionId}/leaderboard`).then((r) => r.data),

  /** Number of distinct investors participating in an auction. */
  getBidderCount: (auctionId: number) =>
    api.get<{ count: number }>(`/bids/auction/${auctionId}/bidders/count`).then((r) => r.data.count),

  /**
   * The signed-in investor's own bids across every auction, already annotated with
   * idea title, auction status and whether the bid is leading or won.
   */
  getMyBids: (params?: { page?: number; size?: number }) =>
    api.get<InvestorBid[]>("/bids/my", { params }).then((r) => r.data),
};
