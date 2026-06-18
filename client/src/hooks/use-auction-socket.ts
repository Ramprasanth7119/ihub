"use client";

import { useEffect, useState } from "react";
import { wsService } from "@/services/websocket.service";
import type { BidUpdate, LeaderboardEntry } from "@/types";

export function useAuctionSocket(auctionId: number) {
  const [latestBid, setLatestBid] = useState<BidUpdate | null>(null);
  const [leaderboard, setLeaderboard] = useState<LeaderboardEntry[]>([]);
  const [connected, setConnected] = useState(false);

  useEffect(() => {
    let unsubBids: (() => void) | undefined;
    let unsubLeaderboard: (() => void) | undefined;

    wsService.connect(() => {
      setConnected(true);
      unsubBids = wsService.subscribeAuctionBids(auctionId, setLatestBid);
      unsubLeaderboard = wsService.subscribeAuctionLeaderboard(auctionId, setLeaderboard);
    });

    return () => {
      unsubBids?.();
      unsubLeaderboard?.();
      setConnected(false);
    };
  }, [auctionId]);

  return { latestBid, leaderboard, connected };
}
