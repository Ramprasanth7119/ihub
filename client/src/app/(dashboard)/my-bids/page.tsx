"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { bidService } from "@/services/bid.service";
import { PageHeader } from "@/components/shared/page-header";
import { EmptyState } from "@/components/shared/empty-state";
import { ErrorState } from "@/components/shared/error-state";
import { CountdownTimer } from "@/components/shared/countdown-timer";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Gavel, Trophy, TrendingUp, TrendingDown, ArrowRight, Clock } from "lucide-react";
import { format } from "date-fns";
import { useRequireAuth } from "@/hooks/use-auth";
import { cn, formatCurrency } from "@/lib/utils";
import type { InvestorBid } from "@/types";

const PAGE_SIZE = 20;

type Tab = "all" | "leading" | "outbid" | "won";

const TABS: { value: Tab; label: string }[] = [
  { value: "all", label: "All bids" },
  { value: "leading", label: "Leading" },
  { value: "outbid", label: "Outbid" },
  { value: "won", label: "Won" },
];

/**
 * An investor's own bidding history.
 *
 * <p>Backed by a single `/bids/my` request that returns each bid already annotated
 * with the idea title, auction status and whether it is leading or won — so the
 * page costs one request regardless of how many auctions are involved.</p>
 */
export default function MyBidsPage() {
  const router = useRouter();
  const auth = useRequireAuth(["INVESTOR"]);
  const [tab, setTab] = useState<Tab>("all");
  const [page, setPage] = useState(0);

  const { data, isLoading, isError, error, refetch, isFetching } = useQuery({
    queryKey: ["bids", "mine", page],
    queryFn: () => bidService.getMyBids({ page, size: PAGE_SIZE }),
    enabled: auth.isAuthenticated() && auth.role === "INVESTOR",
    placeholderData: (previous) => previous,
  });

  const bids = useMemo(() => data ?? [], [data]);

  const filtered = useMemo(() => {
    switch (tab) {
      case "leading":
        return bids.filter((bid) => bid.leading && bid.auctionStatus === "ACTIVE");
      case "outbid":
        return bids.filter((bid) => !bid.leading && bid.auctionStatus === "ACTIVE");
      case "won":
        return bids.filter((bid) => bid.won);
      default:
        return bids;
    }
  }, [bids, tab]);

  const stats = useMemo(() => {
    const active = bids.filter((bid) => bid.auctionStatus === "ACTIVE");
    return {
      total: bids.length,
      leading: active.filter((bid) => bid.leading).length,
      won: bids.filter((bid) => bid.won).length,
      committed: active
        .filter((bid) => bid.leading)
        .reduce((sum, bid) => sum + bid.amount, 0),
    };
  }, [bids]);

  if (!auth.isHydrated) {
    return <Skeleton className="h-64 w-full" />;
  }

  return (
    <div>
      <PageHeader
        title="My Bids"
        description="Every bid you've placed, and where each auction stands right now."
        action={
          <Button variant="secondary" onClick={() => router.push("/auctions")}>
            Browse auctions
            <ArrowRight className="ml-2 h-4 w-4" />
          </Button>
        }
      />

      <div className="mb-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatTile icon={Gavel} label="Bids placed" value={stats.total.toLocaleString()} />
        <StatTile
          icon={TrendingUp}
          label="Currently leading"
          value={stats.leading.toLocaleString()}
          accent="text-emerald-400"
        />
        <StatTile
          icon={Trophy}
          label="Auctions won"
          value={stats.won.toLocaleString()}
          accent="text-amber-400"
        />
        <StatTile
          icon={Clock}
          label="Value at stake"
          value={formatCurrency(stats.committed)}
          hint="your leading bids on live auctions"
        />
      </div>

      <div className="mb-6 inline-flex flex-wrap gap-1 rounded-xl border border-white/10 bg-white/[0.03] p-1">
        {TABS.map((item) => (
          <button
            key={item.value}
            type="button"
            onClick={() => setTab(item.value)}
            aria-pressed={tab === item.value}
            className={cn(
              "rounded-lg px-4 py-2 text-sm font-medium transition-colors",
              tab === item.value
                ? "bg-violet-500/20 text-white"
                : "text-slate-400 hover:text-white"
            )}
          >
            {item.label}
          </button>
        ))}
      </div>

      {isError ? (
        <ErrorState error={error} onRetry={() => refetch()} />
      ) : isLoading ? (
        <div className="space-y-3">
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className="h-28 w-full rounded-2xl" />
          ))}
        </div>
      ) : filtered.length === 0 ? (
        <EmptyState
          icon={Gavel}
          title={tab === "all" ? "You haven't bid yet" : "Nothing here"}
          description={
            tab === "all"
              ? "Find an idea worth backing and place your first bid — it will show up here."
              : tab === "leading"
                ? "You're not the top bidder on any live auction right now."
                : tab === "outbid"
                  ? "You haven't been outbid on any live auction."
                  : "You haven't won an auction yet."
          }
          actionLabel={tab === "all" ? "Discover ideas" : undefined}
          onAction={tab === "all" ? () => router.push("/search") : undefined}
        />
      ) : (
        <div className="space-y-3">
          {filtered.map((bid) => (
            <BidRow key={bid.bidId} bid={bid} />
          ))}
        </div>
      )}

      {bids.length >= PAGE_SIZE && (
        <div className="mt-6 flex justify-center gap-2">
          <Button
            variant="outline"
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            disabled={page === 0 || isFetching}
          >
            Previous
          </Button>
          <Button
            variant="outline"
            onClick={() => setPage((p) => p + 1)}
            disabled={bids.length < PAGE_SIZE || isFetching}
          >
            Next
          </Button>
        </div>
      )}
    </div>
  );
}

function BidRow({ bid }: { bid: InvestorBid }) {
  const isLive = bid.auctionStatus === "ACTIVE";
  const shortfall = bid.highestBid - bid.amount;

  return (
    <Card className="transition-colors hover:border-violet-500/30">
      <CardContent className="flex flex-col gap-4 p-5 sm:flex-row sm:items-center sm:justify-between">
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <Link
              href={`/auctions/${bid.auctionId}`}
              className="truncate font-semibold text-white hover:text-violet-300"
            >
              {bid.ideaTitle}
            </Link>
            <StatusPill bid={bid} />
          </div>

          <div className="mt-2 flex flex-wrap items-center gap-x-5 gap-y-1 text-sm text-slate-400">
            <span>
              Your bid{" "}
              <strong className="font-semibold text-white">{formatCurrency(bid.amount)}</strong>
            </span>
            {isLive && !bid.leading && (
              <span className="text-amber-400">
                {formatCurrency(shortfall)} behind the leader
              </span>
            )}
            <span>{format(new Date(bid.placedAt), "dd MMM yyyy, HH:mm")}</span>
          </div>
        </div>

        <div className="flex items-center gap-4 sm:flex-col sm:items-end sm:gap-1">
          {isLive ? (
            <>
              <span className="text-xs uppercase tracking-wide text-slate-500">Ends in</span>
              <CountdownTimer endTime={bid.endTime} className="text-sm" />
            </>
          ) : (
            <span className="text-sm text-slate-500">
              Ended {format(new Date(bid.endTime), "dd MMM yyyy")}
            </span>
          )}
          <Button asChild variant="ghost" size="sm">
            <Link href={`/auctions/${bid.auctionId}`}>
              View
              <ArrowRight className="ml-1 h-3 w-3" />
            </Link>
          </Button>
        </div>
      </CardContent>
    </Card>
  );
}

function StatusPill({ bid }: { bid: InvestorBid }) {
  if (bid.won) {
    return (
      <Badge variant="warning" className="gap-1">
        <Trophy className="h-3 w-3" />
        Won
      </Badge>
    );
  }
  if (bid.auctionStatus === "CANCELLED") {
    return <Badge variant="muted">Auction cancelled</Badge>;
  }
  if (bid.auctionStatus !== "ACTIVE") {
    return <Badge variant="muted">Not won</Badge>;
  }
  return bid.leading ? (
    <Badge variant="success" className="gap-1">
      <TrendingUp className="h-3 w-3" />
      Leading
    </Badge>
  ) : (
    <Badge variant="muted" className="gap-1">
      <TrendingDown className="h-3 w-3" />
      Outbid
    </Badge>
  );
}

function StatTile({
  icon: Icon,
  label,
  value,
  accent,
  hint,
}: {
  icon: typeof Gavel;
  label: string;
  value: string;
  accent?: string;
  hint?: string;
}) {
  return (
    <Card>
      <CardContent className="p-5">
        <div className="flex items-center gap-2 text-slate-400">
          <Icon className="h-4 w-4" />
          <p className="text-sm">{label}</p>
        </div>
        <p className={cn("mt-1 text-2xl font-bold text-white", accent)}>{value}</p>
        {hint && <p className="mt-1 text-xs text-slate-500">{hint}</p>}
      </CardContent>
    </Card>
  );
}
