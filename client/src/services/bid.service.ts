import api from "@/lib/axios";
import type { BidHistoryEntry, BidResponse, HighestBid, LeaderboardEntry } from "@/types";

export const bidService = {
  placeBid: (auctionId: number, amount: number) =>
    api.post<BidResponse>("/bids", { auctionId, amount }).then((r) => r.data),

  getHistory: (auctionId: number) =>
    api.get<BidHistoryEntry[]>(`/bids/auction/${auctionId}/history`).then((r) => r.data),

  getHighest: (auctionId: number) =>
    api.get<HighestBid>(`/bids/auction/${auctionId}/highest`).then((r) => r.data),

  getLeaderboard: (auctionId: number) =>
    api.get<LeaderboardEntry[]>(`/bids/auction/${auctionId}/leaderboard`).then((r) => r.data),
};
