import api from "@/lib/axios";
import type { Auction, AuctionHistoryEntry, AuctionWinner } from "@/types";

export const auctionService = {
  getAll: (status?: string) =>
    api.get<Auction[]>("/auctions", { params: status ? { status } : {} }).then((r) => r.data),

  getById: (id: number) => api.get<Auction>(`/auctions/${id}`).then((r) => r.data),

  create: (data: {
    ideaId: number;
    startTime: string;
    endTime: string;
    minBidIncrement?: number;
  }) => api.post<Auction>("/auctions", data).then((r) => r.data),

  start: (id: number) => api.post<Auction>(`/auctions/${id}/start`).then((r) => r.data),

  close: (id: number) => api.post<Auction>(`/auctions/${id}/close`).then((r) => r.data),

  getWinner: (id: number) =>
    api.get<AuctionWinner>(`/auctions/${id}/winner`).then((r) => r.data),

  getHistory: (id: number) =>
    api.get<AuctionHistoryEntry[]>(`/auctions/${id}/history`).then((r) => r.data),
};
