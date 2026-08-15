"use client";

import { useQuery } from "@tanstack/react-query";
import { adminService } from "@/services/admin.service";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { ErrorState } from "@/components/shared/error-state";
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  LineChart,
  Line,
} from "recharts";
import {
  TrendingUp,
  TrendingDown,
  Minus,
  Gavel,
  Users,
  Lightbulb,
  Trophy,
  type LucideIcon,
} from "lucide-react";
import { cn, formatCurrency } from "@/lib/utils";
import type { AdminMetrics } from "@/types";

/** Shared palette so both charts read as one system rather than two blue defaults. */
const CHART_COLORS = {
  primary: "#7c3aed",
  secondary: "#0ea5e9",
  grid: "#e2e8f0",
  axis: "#64748b",
};

export default function AdminReportsPage() {
  const {
    data: metrics,
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: ["admin", "metrics"],
    queryFn: () => adminService.getMetrics(),
  });

  const { data: charts } = useQuery({
    queryKey: ["admin", "dashboard-charts"],
    queryFn: () => adminService.getDashboardCharts(),
  });

  if (isLoading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-10 w-48" />
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className="h-28 w-full" />
          ))}
        </div>
        <div className="grid gap-4 md:grid-cols-2">
          <Skeleton className="h-80 w-full" />
          <Skeleton className="h-80 w-full" />
        </div>
      </div>
    );
  }

  if (isError || !metrics) {
    return (
      <div className="space-y-6">
        <PageHeading />
        <ErrorState error={error} onRetry={() => refetch()} />
      </div>
    );
  }

  const growth = userGrowth(metrics);

  return (
    <div className="space-y-6">
      <PageHeading />

      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        {/* Previously this tile showed the bid *count* with a currency prefix and
            called it revenue. It now shows the value actually settled by decided
            auctions, with total bidding activity as the secondary figure. */}
        <SummaryCard
          title="Settled value"
          value={formatCurrency(metrics.settledValue)}
          hint={`${metrics.completedAuctionsWithWinner.toLocaleString()} decided auctions`}
          icon={Trophy}
        />
        <SummaryCard
          title="Total bid value"
          value={formatCurrency(metrics.totalBidValue)}
          hint={`across ${metrics.totalBids.toLocaleString()} bids`}
          icon={Gavel}
        />
        <SummaryCard
          title="Active users"
          value={metrics.activeUsers.toLocaleString()}
          hint={`of ${metrics.totalUsers.toLocaleString()} registered`}
          icon={Users}
        />
        <SummaryCard
          title="User growth"
          value={growth.label}
          hint={growth.hint}
          icon={growth.icon}
          accent={growth.accent}
        />
      </div>

      <div className="grid gap-4 md:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Ideas by category</CardTitle>
          </CardHeader>
          <CardContent>
            {charts?.ideasByCategory?.length ? (
              <ResponsiveContainer width="100%" height={300}>
                <BarChart data={charts.ideasByCategory}>
                  <CartesianGrid strokeDasharray="3 3" stroke={CHART_COLORS.grid} vertical={false} />
                  <XAxis
                    dataKey="category"
                    stroke={CHART_COLORS.axis}
                    tick={{ fontSize: 12 }}
                    tickLine={false}
                  />
                  <YAxis
                    stroke={CHART_COLORS.axis}
                    tick={{ fontSize: 12 }}
                    tickLine={false}
                    axisLine={false}
                    allowDecimals={false}
                  />
                  <Tooltip
                    cursor={{ fill: "rgba(124,58,237,0.06)" }}
                    contentStyle={{ borderRadius: 8, border: `1px solid ${CHART_COLORS.grid}` }}
                  />
                  <Bar dataKey="count" fill={CHART_COLORS.primary} radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            ) : (
              <ChartEmpty message="No ideas have been categorised yet." />
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Auctions per month</CardTitle>
          </CardHeader>
          <CardContent>
            {charts?.monthlyAuctions?.length ? (
              <ResponsiveContainer width="100%" height={300}>
                <LineChart data={charts.monthlyAuctions}>
                  <CartesianGrid strokeDasharray="3 3" stroke={CHART_COLORS.grid} vertical={false} />
                  <XAxis
                    dataKey="month"
                    stroke={CHART_COLORS.axis}
                    tick={{ fontSize: 12 }}
                    tickLine={false}
                  />
                  <YAxis
                    stroke={CHART_COLORS.axis}
                    tick={{ fontSize: 12 }}
                    tickLine={false}
                    axisLine={false}
                    allowDecimals={false}
                  />
                  <Tooltip
                    contentStyle={{ borderRadius: 8, border: `1px solid ${CHART_COLORS.grid}` }}
                  />
                  <Line
                    type="monotone"
                    dataKey="count"
                    stroke={CHART_COLORS.secondary}
                    strokeWidth={2}
                    dot={{ r: 3 }}
                    activeDot={{ r: 5 }}
                  />
                </LineChart>
              </ResponsiveContainer>
            ) : (
              <ChartEmpty message="No auctions have run yet." />
            )}
          </CardContent>
        </Card>
      </div>

      <div className="grid gap-4 md:grid-cols-3">
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-base">
              <Users className="h-4 w-4" />
              User distribution
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <StatRow label="Creators" value={metrics.totalCreators} total={metrics.totalUsers} />
            <StatRow label="Investors" value={metrics.totalInvestors} total={metrics.totalUsers} />
            <StatRow label="Admins" value={metrics.totalAdmins} total={metrics.totalUsers} />
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-base">
              <Lightbulb className="h-4 w-4" />
              Idea status
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <StatRow label="Published" value={metrics.publishedIdeas} total={metrics.totalIdeas} />
            <StatRow label="Draft" value={metrics.draftIdeas} total={metrics.totalIdeas} />
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-base">
              <Gavel className="h-4 w-4" />
              Auction status
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <StatRow label="Live" value={metrics.activeAuctions} total={metrics.totalAuctions} />
            <StatRow
              label="Upcoming"
              value={metrics.scheduledAuctions}
              total={metrics.totalAuctions}
            />
            <StatRow label="Completed" value={metrics.closedAuctions} total={metrics.totalAuctions} />
            <StatRow
              label="Cancelled"
              value={metrics.cancelledAuctions}
              total={metrics.totalAuctions}
            />
          </CardContent>
        </Card>
      </div>

      <p className="text-xs text-gray-500">
        Settled value counts winning bids on auctions that have closed with a winner. Total bid
        value counts every bid placed, so a competitive auction contributes more than it settles
        for.
      </p>
    </div>
  );
}

/**
 * Month-over-month registration growth, derived from real signup timestamps.
 * The previous version displayed a hardcoded 15.3%.
 */
function userGrowth(metrics: AdminMetrics): {
  label: string;
  hint: string;
  icon: LucideIcon;
  accent: string;
} {
  const current = metrics.newUsersLast30Days;
  const previous = metrics.newUsersPrevious30Days;

  if (previous === 0) {
    return current === 0
      ? { label: "No signups", hint: "in the last 30 days", icon: Minus, accent: "text-gray-500" }
      : {
          label: `+${current}`,
          hint: "new users, first full period",
          icon: TrendingUp,
          accent: "text-emerald-600",
        };
  }

  const change = ((current - previous) / previous) * 100;
  const rising = change >= 0;

  return {
    label: `${rising ? "+" : ""}${change.toFixed(1)}%`,
    hint: `${current} vs ${previous} in the prior 30 days`,
    icon: rising ? TrendingUp : TrendingDown,
    accent: rising ? "text-emerald-600" : "text-red-600",
  };
}

function PageHeading() {
  return (
    <div>
      <h1 className="text-3xl font-bold text-gray-900">Reports</h1>
      <p className="mt-2 text-gray-600">Platform performance, measured from live data.</p>
    </div>
  );
}

function SummaryCard({
  title,
  value,
  hint,
  icon: Icon,
  accent,
}: {
  title: string;
  value: string;
  hint?: string;
  icon: LucideIcon;
  accent?: string;
}) {
  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
        <CardTitle className="text-sm font-medium text-gray-600">{title}</CardTitle>
        <Icon className={cn("h-4 w-4 text-gray-400", accent)} />
      </CardHeader>
      <CardContent>
        <div className={cn("text-2xl font-bold text-gray-900", accent)}>{value}</div>
        {hint && <p className="mt-1 text-xs text-gray-500">{hint}</p>}
      </CardContent>
    </Card>
  );
}

function StatRow({ label, value, total }: { label: string; value: number; total: number }) {
  const percentage = total > 0 ? (value / total) * 100 : 0;
  return (
    <div>
      <div className="flex items-center justify-between text-sm">
        <span className="text-gray-600">{label}</span>
        <span className="font-medium text-gray-900">
          {value.toLocaleString()}
          <span className="ml-1.5 text-xs font-normal text-gray-400">
            {percentage.toFixed(1)}%
          </span>
        </span>
      </div>
      {/* A bar makes the share legible at a glance; the number alone requires
          mental arithmetic against the total. */}
      <div className="mt-1.5 h-1.5 w-full overflow-hidden rounded-full bg-gray-100">
        <div
          className="h-full rounded-full bg-violet-500"
          style={{ width: `${Math.min(100, percentage)}%` }}
        />
      </div>
    </div>
  );
}

function ChartEmpty({ message }: { message: string }) {
  return (
    <div className="flex h-[300px] items-center justify-center rounded-lg border border-dashed border-gray-200">
      <p className="text-sm text-gray-500">{message}</p>
    </div>
  );
}
