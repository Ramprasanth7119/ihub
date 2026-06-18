"use client";

import { useQuery, useQueries } from "@tanstack/react-query";
import { Gavel, Lightbulb, TrendingUp, Users } from "lucide-react";
import {
  Bar,
  BarChart,
  Cell,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { PageHeader } from "@/components/shared/page-header";
import { StatCard } from "@/components/shared/stat-card";
import { Skeleton } from "@/components/ui/skeleton";
import { ideaService } from "@/services/idea.service";
import { auctionService } from "@/services/auction.service";
import { bidService } from "@/services/bid.service";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { formatCurrency } from "@/lib/utils";
import Link from "next/link";
import { Button } from "@/components/ui/button";

const CHART_COLORS = ["#8b5cf6", "#6366f1", "#3b82f6", "#06b6d4", "#10b981"];

export function CreatorDashboard() {
  const { data: ideas, isLoading: ideasLoading } = useQuery({
    queryKey: ["ideas", "mine"],
    queryFn: () => ideaService.getAll({ mine: true }),
  });

  const { data: auctions, isLoading: auctionsLoading } = useQuery({
    queryKey: ["auctions"],
    queryFn: () => auctionService.getAll(),
  });

  const myIdeaIds = new Set(ideas?.map((i) => i.id) ?? []);
  const myAuctions = auctions?.filter((a) => myIdeaIds.has(a.ideaId)) ?? [];
  const activeAuctions = myAuctions.filter((a) => a.status === "ACTIVE");
  const bidHistoryQueries = useQueries({
    queries: myAuctions.map((auction) => ({
      queryKey: ["bids", auction.id, "history"],
      queryFn: () => bidService.getHistory(auction.id),
    })),
  });

  const totalBidsReceived = bidHistoryQueries.reduce(
    (sum, q) => sum + (q.data?.length ?? 0),
    0
  );

  const categoryData = ideas
    ? Object.entries(
        ideas.reduce<Record<string, number>>((acc, idea) => {
          acc[idea.category] = (acc[idea.category] ?? 0) + 1;
          return acc;
        }, {})
      ).map(([name, value]) => ({ name, value }))
    : [];

  const performanceData = myAuctions.map((a) => ({
    name: `Auction ${a.id}`,
    status: a.status === "ACTIVE" ? 1 : a.status === "CLOSED" ? 2 : 0,
  }));

  const isLoading = ideasLoading || auctionsLoading;

  return (
    <div>
      <PageHeader
        title="Creator Dashboard"
        description="Track your ideas, auctions, and investor interest."
        action={
          <Button asChild>
            <Link href="/ideas/new">Submit Idea</Link>
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
          <StatCard title="Total Ideas" value={ideas?.length ?? 0} icon={Lightbulb} />
          <StatCard title="Active Auctions" value={activeAuctions.length} icon={Gavel} />
          <StatCard title="Total Bids Received" value={totalBidsReceived} icon={TrendingUp} />
          <StatCard
            title="Published Ideas"
            value={ideas?.filter((i) => i.status === "PUBLISHED").length ?? 0}
            icon={Users}
          />
        </div>
      )}

      <div className="mt-8 grid gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Category Distribution</CardTitle>
          </CardHeader>
          <CardContent>
            {categoryData.length > 0 ? (
              <ResponsiveContainer width="100%" height={240}>
                <PieChart>
                  <Pie
                    data={categoryData}
                    dataKey="value"
                    nameKey="name"
                    cx="50%"
                    cy="50%"
                    innerRadius={60}
                    outerRadius={90}
                  >
                    {categoryData.map((_, i) => (
                      <Cell key={i} fill={CHART_COLORS[i % CHART_COLORS.length]} />
                    ))}
                  </Pie>
                  <Tooltip
                    contentStyle={{ background: "#0f172a", border: "1px solid rgba(255,255,255,0.1)", borderRadius: 12 }}
                  />
                </PieChart>
              </ResponsiveContainer>
            ) : (
              <p className="py-12 text-center text-sm text-slate-500">No ideas yet</p>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Auction Performance</CardTitle>
          </CardHeader>
          <CardContent>
            {performanceData.length > 0 ? (
              <ResponsiveContainer width="100%" height={240}>
                <BarChart data={performanceData}>
                  <XAxis dataKey="name" tick={{ fill: "#64748b", fontSize: 11 }} />
                  <YAxis tick={{ fill: "#64748b", fontSize: 11 }} />
                  <Tooltip
                    contentStyle={{ background: "#0f172a", border: "1px solid rgba(255,255,255,0.1)", borderRadius: 12 }}
                  />
                  <Bar dataKey="status" fill="#8b5cf6" radius={[6, 6, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            ) : (
              <p className="py-12 text-center text-sm text-slate-500">No auctions yet</p>
            )}
          </CardContent>
        </Card>
      </div>

      <Card className="mt-6">
        <CardHeader>
          <CardTitle>Recent Ideas</CardTitle>
        </CardHeader>
        <CardContent>
          {ideas && ideas.length > 0 ? (
            <div className="space-y-3">
              {ideas.slice(0, 5).map((idea) => (
                <Link
                  key={idea.id}
                  href={`/ideas/${idea.id}`}
                  className="flex items-center justify-between rounded-xl bg-white/5 px-4 py-3 transition-colors hover:bg-white/10"
                >
                  <div>
                    <p className="font-medium text-white">{idea.title}</p>
                    <p className="text-xs text-slate-500">{idea.category} · {idea.status}</p>
                  </div>
                  <span className="text-sm text-violet-400">
                    {formatCurrency(idea.basePrice)}
                  </span>
                </Link>
              ))}
            </div>
          ) : (
            <p className="text-center text-sm text-slate-500 py-8">Submit your first idea to get started</p>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
