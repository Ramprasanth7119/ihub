"use client";

import { use, useEffect, useState } from "react";
import Link from "next/link";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { adminService } from "@/services/admin.service";
import { auctionService } from "@/services/auction.service";
import { bidService } from "@/services/bid.service";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { ErrorState } from "@/components/shared/error-state";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { CountdownTimer } from "@/components/shared/countdown-timer";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  ArrowLeft,
  Play,
  Square,
  XCircle,
  Trophy,
  Users,
  Gavel,
  TrendingUp,
  Radio,
  WifiOff,
  Clock,
} from "lucide-react";
import { format } from "date-fns";
import { toast } from "sonner";
import { useAuctionSocket } from "@/hooks/use-auction-socket";
import { getErrorMessage, isNotFound } from "@/lib/api-error";
import { cn, formatAuctionStatus, formatCurrency, getAdminStatusColor } from "@/lib/utils";

type PendingAction = "start" | "end" | "cancel" | null;

/**
 * Live monitoring for a single auction.
 *
 * <p>Subscribes to the auction's WebSocket topics so bids appear as they land, and
 * exposes the lifecycle controls in context — previously an admin could only act on
 * an auction from the list, with no view of who was bidding.</p>
 */
export default function AdminAuctionDetailPage({ params }: { params: Promise<{ id: string }> }) {
  // Route params are Promises in Next.js 16.
  const { id } = use(params);
  const auctionId = Number(id);

  const queryClient = useQueryClient();
  const [pending, setPending] = useState<PendingAction>(null);

  const {
    data: auction,
    isLoading,
    isError,
    error,
    refetch: refetchAuction,
  } = useQuery({
    queryKey: ["admin", "auction", auctionId],
    queryFn: () => adminService.getAuction(auctionId),
    enabled: Number.isFinite(auctionId),
  });

  const { data: leaderboard = [], refetch: refetchLeaderboard } = useQuery({
    queryKey: ["auction", auctionId, "leaderboard"],
    queryFn: () => bidService.getLeaderboard(auctionId),
    enabled: Number.isFinite(auctionId),
  });

  const { data: bidHistory = [], refetch: refetchHistory } = useQuery({
    queryKey: ["auction", auctionId, "bid-history"],
    queryFn: () => bidService.getHistory(auctionId, { size: 50 }),
    enabled: Number.isFinite(auctionId),
  });

  const { data: bidderCount = 0, refetch: refetchBidders } = useQuery({
    queryKey: ["auction", auctionId, "bidder-count"],
    queryFn: () => bidService.getBidderCount(auctionId),
    enabled: Number.isFinite(auctionId),
  });

  const { data: timeline = [] } = useQuery({
    queryKey: ["auction", auctionId, "history"],
    queryFn: () => auctionService.getHistory(auctionId),
    enabled: Number.isFinite(auctionId),
  });

  // 404 here means the auction has no winner yet, which is normal while it runs.
  const { data: winner } = useQuery({
    queryKey: ["auction", auctionId, "winner"],
    queryFn: () => auctionService.getWinner(auctionId),
    enabled: Number.isFinite(auctionId) && auction?.status === "CLOSED",
    retry: (failureCount, err) => !isNotFound(err) && failureCount < 2,
  });

  const { latestBid, connected } = useAuctionSocket(auctionId);

  // A pushed bid only signals that something changed; the authoritative figures
  // are re-read from the API rather than patched into local state.
  useEffect(() => {
    if (!latestBid) return;
    refetchLeaderboard();
    refetchHistory();
    refetchBidders();
    refetchAuction();
  }, [latestBid, refetchLeaderboard, refetchHistory, refetchBidders, refetchAuction]);

  const actionMutation = useMutation({
    mutationFn: (action: NonNullable<PendingAction>) => {
      if (action === "start") return adminService.startAuction(auctionId);
      if (action === "end") return adminService.endAuction(auctionId);
      return adminService.cancelAuction(auctionId);
    },
    onSuccess: (_result, action) => {
      queryClient.invalidateQueries({ queryKey: ["admin", "auction", auctionId] });
      queryClient.invalidateQueries({ queryKey: ["auction", auctionId] });
      queryClient.invalidateQueries({ queryKey: ["admin", "auctions"] });
      setPending(null);
      toast.success(
        action === "start"
          ? "Auction started"
          : action === "end"
            ? "Auction closed and the winner notified"
            : "Auction cancelled"
      );
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  if (isLoading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-10 w-64" />
        <div className="grid gap-4 sm:grid-cols-4">
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className="h-28 w-full" />
          ))}
        </div>
        <Skeleton className="h-96 w-full" />
      </div>
    );
  }

  if (isError || !auction) {
    return (
      <div className="space-y-6">
        <BackLink />
        <ErrorState error={error} onRetry={() => refetchAuction()} />
      </div>
    );
  }

  const isLive = auction.status === "ACTIVE";
  const isUpcoming = auction.status === "SCHEDULED" || auction.status === "UPCOMING";
  const topBid = leaderboard[0];
  const minimumNextBid = topBid ? topBid.highestBid : undefined;

  return (
    <div className="space-y-6">
      <BackLink />

      <div className="flex flex-wrap items-start justify-between gap-4">
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-3">
            <h1 className="text-3xl font-bold text-gray-900">{auction.ideaTitle}</h1>
            <Badge variant="outline" className={getAdminStatusColor(auction.status)}>
              {formatAuctionStatus(auction.status)}
            </Badge>
            {isLive && (
              <span
                className={cn(
                  "inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-medium",
                  connected ? "bg-emerald-50 text-emerald-700" : "bg-slate-100 text-slate-600"
                )}
              >
                {connected ? (
                  <>
                    <Radio className="h-3 w-3 animate-pulse" />
                    Live
                  </>
                ) : (
                  <>
                    <WifiOff className="h-3 w-3" />
                    Reconnecting…
                  </>
                )}
              </span>
            )}
          </div>
          <p className="mt-1 text-sm text-gray-500">
            Auction #{auction.id} ·{" "}
            <Link href={`/ideas/${auction.ideaId}`} className="hover:underline">
              Idea #{auction.ideaId}
            </Link>
          </p>
        </div>

        <div className="flex flex-wrap gap-2">
          {isUpcoming && (
            <Button onClick={() => setPending("start")}>
              <Play className="mr-2 h-4 w-4" />
              Start now
            </Button>
          )}
          {isLive && (
            <Button onClick={() => setPending("end")}>
              <Square className="mr-2 h-4 w-4" />
              End &amp; pick winner
            </Button>
          )}
          {(isUpcoming || isLive) && (
            <Button variant="outline" onClick={() => setPending("cancel")}>
              <XCircle className="mr-2 h-4 w-4" />
              Cancel
            </Button>
          )}
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <MetricTile
          icon={TrendingUp}
          label="Highest bid"
          value={auction.highestBid ? formatCurrency(auction.highestBid) : "No bids yet"}
          accent="text-emerald-600"
        />
        <MetricTile icon={Gavel} label="Total bids" value={auction.bidCount.toLocaleString()} />
        <MetricTile icon={Users} label="Unique bidders" value={bidderCount.toLocaleString()} />
        <Card>
          <CardContent className="p-5">
            <div className="flex items-center gap-2 text-gray-500">
              <Clock className="h-4 w-4" />
              <p className="text-sm">{isLive ? "Time remaining" : "Window"}</p>
            </div>
            {isLive ? (
              <CountdownTimer
                endTime={auction.endTime}
                className="mt-1 block text-xl font-bold text-emerald-600"
                onComplete={() => refetchAuction()}
              />
            ) : (
              <p className="mt-1 text-sm font-medium text-gray-900">
                {format(new Date(auction.startTime), "dd MMM, HH:mm")} →{" "}
                {format(new Date(auction.endTime), "dd MMM, HH:mm")}
              </p>
            )}
          </CardContent>
        </Card>
      </div>

      {winner && (
        <Card className="border-amber-200 bg-amber-50">
          <CardContent className="flex flex-wrap items-center gap-4 p-5">
            <span className="flex h-11 w-11 items-center justify-center rounded-full bg-amber-100">
              <Trophy className="h-5 w-5 text-amber-600" />
            </span>
            <div>
              <p className="text-sm text-amber-800">Winner</p>
              <p className="text-lg font-semibold text-amber-900">
                {winner.winnerName} · {formatCurrency(winner.winningBid)}
              </p>
            </div>
          </CardContent>
        </Card>
      )}

      <div className="grid gap-6 lg:grid-cols-5">
        <Card className="lg:col-span-3">
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Trophy className="h-5 w-5" />
              Leaderboard
            </CardTitle>
          </CardHeader>
          <CardContent className="p-0">
            {leaderboard.length === 0 ? (
              <EmptyRow message="No bids placed yet." />
            ) : (
              <div className="overflow-x-auto">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead className="w-16">Rank</TableHead>
                      <TableHead>Investor</TableHead>
                      <TableHead className="text-right">Best bid</TableHead>
                      <TableHead>Placed</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {leaderboard.map((entry) => (
                      <TableRow
                        key={entry.investorId}
                        className={entry.rank === 1 ? "bg-amber-50/60" : undefined}
                      >
                        <TableCell>
                          <span
                            className={cn(
                              "inline-flex h-7 w-7 items-center justify-center rounded-full text-xs font-bold",
                              entry.rank === 1
                                ? "bg-amber-100 text-amber-700"
                                : "bg-gray-100 text-gray-600"
                            )}
                          >
                            {entry.rank}
                          </span>
                        </TableCell>
                        <TableCell className="font-medium text-gray-900">
                          {entry.investorName}
                        </TableCell>
                        <TableCell className="text-right font-semibold">
                          {formatCurrency(entry.highestBid)}
                        </TableCell>
                        <TableCell className="whitespace-nowrap text-sm text-gray-500">
                          {format(new Date(entry.lastBidAt), "dd MMM, HH:mm")}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>
            )}
          </CardContent>
        </Card>

        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Clock className="h-5 w-5" />
              Lifecycle
            </CardTitle>
          </CardHeader>
          <CardContent>
            {timeline.length === 0 ? (
              <p className="py-6 text-center text-sm text-gray-500">No events recorded.</p>
            ) : (
              <ol className="space-y-4">
                {timeline.map((event, index) => (
                  <li key={event.id} className="flex gap-3">
                    <div className="flex flex-col items-center">
                      <span className="mt-1 h-2.5 w-2.5 rounded-full bg-violet-500" />
                      {index < timeline.length - 1 && (
                        <span className="mt-1 w-px flex-1 bg-gray-200" />
                      )}
                    </div>
                    <div className="pb-1">
                      <p className="text-sm font-medium text-gray-900">
                        {event.eventType.replace(/_/g, " ").toLowerCase()}
                      </p>
                      {event.details && (
                        <p className="text-sm text-gray-600">{event.details}</p>
                      )}
                      <p className="mt-0.5 text-xs text-gray-400">
                        {format(new Date(event.createdAt), "dd MMM yyyy, HH:mm:ss")}
                      </p>
                    </div>
                  </li>
                ))}
              </ol>
            )}
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Gavel className="h-5 w-5" />
            Bid activity
            {isLive && connected && (
              <span className="text-xs font-normal text-emerald-600">· updating live</span>
            )}
          </CardTitle>
        </CardHeader>
        <CardContent className="p-0">
          {bidHistory.length === 0 ? (
            <EmptyRow message="No bids have been placed on this auction." />
          ) : (
            <div className="max-h-96 overflow-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Investor</TableHead>
                    <TableHead className="text-right">Amount</TableHead>
                    <TableHead>Placed at</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {bidHistory.map((bid) => (
                    <TableRow key={bid.bidId}>
                      <TableCell className="font-medium text-gray-900">
                        {bid.investorName}
                      </TableCell>
                      <TableCell
                        className={cn(
                          "text-right font-semibold",
                          minimumNextBid && bid.amount === minimumNextBid
                            ? "text-emerald-700"
                            : "text-gray-700"
                        )}
                      >
                        {formatCurrency(bid.amount)}
                      </TableCell>
                      <TableCell className="whitespace-nowrap text-sm text-gray-500">
                        {format(new Date(bid.placedAt), "dd MMM yyyy, HH:mm:ss")}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          )}
        </CardContent>
      </Card>

      <ConfirmDialog
        open={pending !== null}
        onOpenChange={(open) => !open && setPending(null)}
        title={
          pending === "start"
            ? "Start this auction now?"
            : pending === "end"
              ? "End this auction?"
              : "Cancel this auction?"
        }
        description={`This affects "${auction.ideaTitle}".`}
        detail={
          pending === "end"
            ? auction.bidCount > 0
              ? `The highest of ${auction.bidCount} bid(s) wins. The winner and creator are notified, and this cannot be undone.`
              : "No bids were placed, so this closes without a winner."
            : pending === "cancel"
              ? auction.bidCount > 0
                ? `${auction.bidCount} bid(s) become void and every bidder is notified.`
                : "No bids have been placed yet."
              : "Bidding opens immediately and the creator is notified."
        }
        confirmLabel={
          pending === "start" ? "Start auction" : pending === "end" ? "End auction" : "Cancel auction"
        }
        destructive={pending === "cancel"}
        loading={actionMutation.isPending}
        onConfirm={() => pending && actionMutation.mutate(pending)}
      />
    </div>
  );
}

function BackLink() {
  return (
    <Link
      href="/admin/auctions"
      className="inline-flex items-center gap-2 text-sm text-gray-600 hover:text-gray-900"
    >
      <ArrowLeft className="h-4 w-4" />
      Back to auctions
    </Link>
  );
}

function MetricTile({
  icon: Icon,
  label,
  value,
  accent,
}: {
  icon: typeof Users;
  label: string;
  value: string;
  accent?: string;
}) {
  return (
    <Card>
      <CardContent className="p-5">
        <div className="flex items-center gap-2 text-gray-500">
          <Icon className="h-4 w-4" />
          <p className="text-sm">{label}</p>
        </div>
        <p className={cn("mt-1 text-xl font-bold text-gray-900", accent)}>{value}</p>
      </CardContent>
    </Card>
  );
}

function EmptyRow({ message }: { message: string }) {
  return <p className="px-6 py-10 text-center text-sm text-gray-500">{message}</p>;
}
