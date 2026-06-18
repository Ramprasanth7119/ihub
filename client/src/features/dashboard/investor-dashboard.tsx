"use client";

import { useQuery, useQueries } from "@tanstack/react-query";
import { DollarSign, Gavel, Trophy, TrendingUp } from "lucide-react";
import {
  Area,
  AreaChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { PageHeader } from "@/components/shared/page-header";
import { StatCard } from "@/components/shared/stat-card";
import { Skeleton } from "@/components/ui/skeleton";
import { auctionService } from "@/services/auction.service";
import { bidService } from "@/services/bid.service";
import { searchService } from "@/services/search.service";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import Link from "next/link";
import { useAuthStore } from "@/store/auth-store";
import { formatCurrency } from "@/lib/utils";

export function InvestorDashboard() {
  const userId = useAuthStore((s) => s.user?.id);

  const { data: auctions, isLoading } = useQuery({
    queryKey: ["auctions"],
    queryFn: () => auctionService.getAll(),
  });

  const { data: liveIdeas } = useQuery({
    queryKey: ["search", "live"],
    queryFn: () => searchService.liveIdeas(0, 5),
  });

  const activeAuctions = auctions?.filter((a) => a.status === "ACTIVE") ?? [];
  const closedAuctions = auctions?.filter((a) => a.status === "CLOSED") ?? [];

  const highestBidQueries = useQueries({
    queries: activeAuctions.slice(0, 6).map((auction) => ({
      queryKey: ["bids", auction.id, "highest"],
      queryFn: () => bidService.getHighest(auction.id),
      retry: false,
    })),
  });

  const winnerQueries = useQueries({
    queries: closedAuctions.map((auction) => ({
      queryKey: ["auctions", auction.id, "winner"],
      queryFn: () => auctionService.getWinner(auction.id),
      retry: false,
    })),
  });

  const wonCount = winnerQueries.filter(
    (q) => q.data && userId && q.data.winnerId === userId
  ).length;

  const investmentData = activeAuctions.slice(0, 6).map((auction, i) => ({
    name: `A${auction.id}`,
    potential: highestBidQueries[i]?.data?.amount ?? 0,
  }));

  return (
    <div>
      <PageHeader
        title="Investor Dashboard"
        description="Track your bids, auctions, and portfolio performance."
        action={
          <Button asChild>
            <Link href="/search">Discover Ideas</Link>
          </Button>
        }
      />

      {isLoading ? (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className="h-32" />
          ))}
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <StatCard title="Active Auctions" value={activeAuctions.length} icon={Gavel} />
          <StatCard title="Live Ideas" value={liveIdeas?.totalElements ?? 0} icon={TrendingUp} />
          <StatCard title="Open Opportunities" value={auctions?.filter((a) => a.status === "UPCOMING").length ?? 0} icon={DollarSign} />
          <StatCard title="Auctions Won" value={wonCount} icon={Trophy} />
        </div>
      )}

      <div className="mt-8 grid gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Current Highest Bids</CardTitle>
          </CardHeader>
          <CardContent>
            {investmentData.some((d) => d.potential > 0) ? (
              <ResponsiveContainer width="100%" height={240}>
                <AreaChart data={investmentData}>
                  <defs>
                    <linearGradient id="colorInv" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#8b5cf6" stopOpacity={0.3} />
                      <stop offset="95%" stopColor="#8b5cf6" stopOpacity={0} />
                    </linearGradient>
                  </defs>
                  <XAxis dataKey="name" tick={{ fill: "#64748b", fontSize: 11 }} />
                  <YAxis tick={{ fill: "#64748b", fontSize: 11 }} />
                  <Tooltip
                    contentStyle={{ background: "#0f172a", border: "1px solid rgba(255,255,255,0.1)", borderRadius: 12 }}
                    formatter={(v) => [formatCurrency(Number(v)), "Highest Bid"]}
                  />
                  <Area type="monotone" dataKey="potential" stroke="#8b5cf6" fill="url(#colorInv)" />
                </AreaChart>
              </ResponsiveContainer>
            ) : (
              <p className="py-12 text-center text-sm text-slate-500">No bid data for active auctions yet</p>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Live Auctions</CardTitle>
          </CardHeader>
          <CardContent>
            {activeAuctions.length > 0 ? (
              <div className="space-y-3">
                {activeAuctions.slice(0, 5).map((auction) => (
                  <Link
                    key={auction.id}
                    href={`/auctions/${auction.id}`}
                    className="flex items-center justify-between rounded-xl bg-white/5 px-4 py-3 transition-colors hover:bg-white/10"
                  >
                    <div>
                      <p className="font-medium text-white">Auction #{auction.id}</p>
                      <p className="text-xs text-slate-500">Idea #{auction.ideaId}</p>
                    </div>
                    <span className="rounded-full bg-emerald-500/15 px-2 py-0.5 text-xs text-emerald-400">
                      LIVE
                    </span>
                  </Link>
                ))}
              </div>
            ) : (
              <p className="py-8 text-center text-sm text-slate-500">No active auctions right now</p>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
