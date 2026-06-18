"use client";

import { use, useEffect, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { motion } from "framer-motion";
import { Wifi, WifiOff } from "lucide-react";
import { PageHeader } from "@/components/shared/page-header";
import { CountdownTimer } from "@/components/shared/countdown-timer";
import { ErrorState } from "@/components/shared/error-state";
import { Skeleton } from "@/components/ui/skeleton";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { auctionService } from "@/services/auction.service";
import { ideaService } from "@/services/idea.service";
import { bidService } from "@/services/bid.service";
import { Leaderboard } from "@/features/auctions/leaderboard";
import { BidPanel } from "@/features/auctions/bid-panel";
import { useAuctionSocket } from "@/hooks/use-auction-socket";
import { useAuthStore } from "@/store/auth-store";
import { formatCurrency, formatDate, getStatusColor } from "@/lib/utils";
import type { LeaderboardEntry } from "@/types";

export default function AuctionDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const auctionId = Number(id);
  const role = useAuthStore((s) => s.role);
  const userId = useAuthStore((s) => s.user?.id);

  const { data: auction, isLoading, isError, refetch } = useQuery({
    queryKey: ["auctions", auctionId],
    queryFn: () => auctionService.getById(auctionId),
  });

  const { data: idea } = useQuery({
    queryKey: ["ideas", auction?.ideaId],
    queryFn: () => ideaService.getById(auction!.ideaId),
    enabled: !!auction?.ideaId,
  });

  const { data: highestBid, refetch: refetchHighest } = useQuery({
    queryKey: ["bids", auctionId, "highest"],
    queryFn: () => bidService.getHighest(auctionId),
    enabled: !!auction,
    retry: false,
  });

  const { data: initialLeaderboard, refetch: refetchLeaderboard } = useQuery({
    queryKey: ["bids", auctionId, "leaderboard"],
    queryFn: () => bidService.getLeaderboard(auctionId),
    enabled: !!auction,
  });

  const { data: bidHistory, refetch: refetchBidHistory } = useQuery({
    queryKey: ["bids", auctionId, "history"],
    queryFn: () => bidService.getHistory(auctionId),
    enabled: !!auction,
  });

  const { data: auctionHistory } = useQuery({
    queryKey: ["auctions", auctionId, "history"],
    queryFn: () => auctionService.getHistory(auctionId),
    enabled: !!auction,
  });

  const { data: winner } = useQuery({
    queryKey: ["auctions", auctionId, "winner"],
    queryFn: () => auctionService.getWinner(auctionId),
    enabled: !!auction && auction.status === "CLOSED",
    retry: false,
  });

  const { latestBid, leaderboard: liveLeaderboard, connected } = useAuctionSocket(auctionId);
  const [leaderboard, setLeaderboard] = useState<LeaderboardEntry[]>([]);

  useEffect(() => {
    if (initialLeaderboard) setLeaderboard(initialLeaderboard);
  }, [initialLeaderboard]);

  useEffect(() => {
    if (liveLeaderboard.length > 0) setLeaderboard(liveLeaderboard);
  }, [liveLeaderboard]);

  useEffect(() => {
    if (latestBid) {
      refetchHighest();
      refetchLeaderboard();
      refetchBidHistory();
    }
  }, [latestBid, refetchHighest, refetchLeaderboard, refetchBidHistory]);

  if (isLoading) return <Skeleton className="h-96" />;
  if (isError || !auction) return <ErrorState onRetry={() => refetch()} />;

  const targetTime = auction.status === "UPCOMING" ? auction.startTime : auction.endTime;

  return (
    <div className="mx-auto max-w-6xl">
      <PageHeader
        title={idea?.title ?? `Auction #${auction.id}`}
        description={`Idea auction · ${auction.status}`}
        action={
          <div className="flex items-center gap-2 text-sm">
            {connected ? (
              <span className="flex items-center gap-1 text-emerald-400">
                <Wifi className="h-4 w-4" /> Live
              </span>
            ) : (
              <span className="flex items-center gap-1 text-slate-500">
                <WifiOff className="h-4 w-4" /> Connecting...
              </span>
            )}
          </div>
        }
      />

      <div className="mb-6 flex flex-wrap gap-2">
        <span className={`rounded-full border px-2.5 py-0.5 text-xs font-medium ${getStatusColor(auction.status)}`}>
          {auction.status}
        </span>
        {idea && <Badge variant="muted">{idea.category}</Badge>}
        {bidHistory && (
          <Badge variant="muted">{bidHistory.length} bids</Badge>
        )}
      </div>

      <div className="grid gap-6 lg:grid-cols-3">
        <div className="space-y-6 lg:col-span-2">
          {idea && (
            <Card>
              <CardHeader>
                <CardTitle>Idea Information</CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-slate-300">{idea.description}</p>
                <div className="mt-4 flex gap-4 text-sm">
                  <span className="text-slate-500">
                    Budget: {formatCurrency(idea.basePrice)}
                    {idea.maxBudget && ` – ${formatCurrency(idea.maxBudget)}`}
                  </span>
                  <Link href={`/ideas/${idea.id}`} className="text-violet-400 hover:underline">
                    View idea
                  </Link>
                </div>
              </CardContent>
            </Card>
          )}

          <Card>
            <CardHeader>
              <CardTitle>Live Leaderboard</CardTitle>
            </CardHeader>
            <CardContent>
              <Leaderboard entries={leaderboard} currentUserId={userId} />
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Bid History</CardTitle>
            </CardHeader>
            <CardContent>
              {bidHistory && bidHistory.length > 0 ? (
                <div className="max-h-64 space-y-2 overflow-y-auto">
                  {bidHistory.map((bid) => (
                    <div
                      key={bid.bidId}
                      className="flex items-center justify-between rounded-lg bg-white/5 px-4 py-2.5 text-sm"
                    >
                      <div>
                        <p className="font-medium text-white">{bid.investorName}</p>
                        <p className="text-xs text-slate-500">{formatDate(bid.placedAt)}</p>
                      </div>
                      <span className="font-semibold text-violet-400">
                        {formatCurrency(bid.amount)}
                      </span>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="py-6 text-center text-sm text-slate-500">No bids placed yet</p>
              )}
            </CardContent>
          </Card>

          {auctionHistory && auctionHistory.length > 0 && (
            <Card>
              <CardHeader>
                <CardTitle>Auction Timeline</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="space-y-3">
                  {auctionHistory.map((event) => (
                    <div key={event.id} className="flex gap-3 text-sm">
                      <div className="mt-1.5 h-2 w-2 shrink-0 rounded-full bg-violet-500" />
                      <div>
                        <p className="font-medium text-white">{event.eventType}</p>
                        <p className="text-slate-400">{event.details}</p>
                        <p className="text-xs text-slate-500">{formatDate(event.createdAt)}</p>
                      </div>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>
          )}
        </div>

        <div className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle>Auction Details</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4 text-sm">
              <div>
                <p className="text-slate-500">Time Remaining</p>
                {auction.status !== "CLOSED" ? (
                  <CountdownTimer endTime={targetTime} className="text-lg" />
                ) : (
                  <span className="text-slate-400">Ended</span>
                )}
              </div>
              <div className="flex justify-between">
                <span className="text-slate-500">Starts</span>
                <span className="text-white">{formatDate(auction.startTime)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-500">Ends</span>
                <span className="text-white">{formatDate(auction.endTime)}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-500">Min Increment</span>
                <span className="text-white">
                  {formatCurrency(auction.minBidIncrement ?? 100)}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-500">Total Bids</span>
                <span className="text-white">{bidHistory?.length ?? 0}</span>
              </div>
              {highestBid && (
                <div className="flex justify-between">
                  <span className="text-slate-500">Current Highest</span>
                  <span className="font-semibold text-emerald-400">
                    {formatCurrency(highestBid.amount)}
                  </span>
                </div>
              )}
              {winner && (
                <div className="rounded-lg border border-amber-500/30 bg-amber-500/10 p-3">
                  <p className="text-xs text-amber-300">Winner</p>
                  <p className="font-semibold text-white">{winner.winnerName}</p>
                  <p className="text-sm text-amber-200">{formatCurrency(winner.winningBid)}</p>
                </div>
              )}
            </CardContent>
          </Card>

          {role === "INVESTOR" && (
            <Card>
              <CardHeader>
                <CardTitle>Place a Bid</CardTitle>
              </CardHeader>
              <CardContent>
                <BidPanel
                  auctionId={auctionId}
                  highestBid={highestBid}
                  minIncrement={auction.minBidIncrement}
                  status={auction.status}
                />
              </CardContent>
            </Card>
          )}

          {latestBid && (
            <motion.div
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              className="rounded-xl border border-violet-500/30 bg-violet-500/10 p-4 text-sm"
            >
              <p className="text-violet-300">New bid placed!</p>
              <p className="mt-1 font-semibold text-white">
                {formatCurrency(latestBid.amount)}
              </p>
            </motion.div>
          )}
        </div>
      </div>
    </div>
  );
}
