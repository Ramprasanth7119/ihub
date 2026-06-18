"use client";

import { useQuery } from "@tanstack/react-query";
import { adminService } from "@/services/admin.service";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, LineChart, Line } from "recharts";
import { TrendingUp, DollarSign, Users, Lightbulb } from "lucide-react";

export default function AdminReportsPage() {
  const { data: metrics, isLoading } = useQuery({
    queryKey: ["admin", "metrics"],
    queryFn: () => adminService.getMetrics(),
  });

  const { data: charts } = useQuery({
    queryKey: ["admin", "dashboard-charts"],
    queryFn: () => adminService.getDashboardCharts(),
  });

  if (isLoading) {
    return <div className="flex items-center justify-center h-96">Loading...</div>;
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold text-gray-900">Reports</h1>
        <p className="mt-2 text-gray-600">Platform performance analytics and insights</p>
      </div>

      {/* Summary Cards */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        <SummaryCard
          title="Total Revenue"
          value={metrics?.totalBids || 0}
          icon={DollarSign}
          prefix="$"
        />
        <SummaryCard
          title="Active Users"
          value={metrics?.activeUsers || 0}
          icon={Users}
        />
        <SummaryCard
          title="Total Ideas"
          value={metrics?.totalIdeas || 0}
          icon={Lightbulb}
        />
        <SummaryCard
          title="Growth Rate"
          value={15.3}
          icon={TrendingUp}
          suffix="%"
        />
      </div>

      {/* Charts */}
      <div className="grid gap-4 md:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Ideas by Category</CardTitle>
          </CardHeader>
          <CardContent>
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={charts?.ideasByCategory}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="category" />
                <YAxis />
                <Tooltip />
                <Bar dataKey="count" fill="#3b82f6" />
              </BarChart>
            </ResponsiveContainer>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Monthly Auctions Trend</CardTitle>
          </CardHeader>
          <CardContent>
            <ResponsiveContainer width="100%" height={300}>
              <LineChart data={charts?.monthlyAuctions}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="month" />
                <YAxis />
                <Tooltip />
                <Line type="monotone" dataKey="count" stroke="#3b82f6" strokeWidth={2} />
              </LineChart>
            </ResponsiveContainer>
          </CardContent>
        </Card>
      </div>

      {/* Detailed Stats */}
      <div className="grid gap-4 md:grid-cols-3">
        <Card>
          <CardHeader>
            <CardTitle>User Distribution</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <StatRow label="Creators" value={metrics?.totalCreators || 0} total={metrics?.totalUsers || 1} />
            <StatRow label="Investors" value={metrics?.totalInvestors || 0} total={metrics?.totalUsers || 1} />
            <StatRow label="Admins" value={metrics?.totalAdmins || 0} total={metrics?.totalUsers || 1} />
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Idea Status</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <StatRow label="Published" value={metrics?.publishedIdeas || 0} total={metrics?.totalIdeas || 1} />
            <StatRow label="Draft" value={metrics?.draftIdeas || 0} total={metrics?.totalIdeas || 1} />
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Auction Status</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <StatRow label="Active" value={metrics?.activeAuctions || 0} total={metrics?.totalAuctions || 1} />
            <StatRow label="Scheduled" value={metrics?.scheduledAuctions || 0} total={metrics?.totalAuctions || 1} />
            <StatRow label="Closed" value={metrics?.closedAuctions || 0} total={metrics?.totalAuctions || 1} />
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

function SummaryCard({
  title,
  value,
  icon: Icon,
  prefix = "",
  suffix = "",
}: {
  title: string;
  value: number;
  icon: any;
  prefix?: string;
  suffix?: string;
}) {
  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
        <CardTitle className="text-sm font-medium">{title}</CardTitle>
        <Icon className="h-4 w-4 text-gray-600" />
      </CardHeader>
      <CardContent>
        <div className="text-2xl font-bold">
          {prefix}
          {typeof value === "number" ? value.toLocaleString() : value}
          {suffix}
        </div>
      </CardContent>
    </Card>
  );
}

function StatRow({ label, value, total }: { label: string; value: number; total: number }) {
  const percentage = total > 0 ? ((value / total) * 100).toFixed(1) : 0;
  return (
    <div className="flex items-center justify-between">
      <span className="text-sm text-gray-600">{label}</span>
      <div className="flex items-center gap-2">
        <span className="font-medium">{value}</span>
        <span className="text-xs text-gray-400">({percentage}%)</span>
      </div>
    </div>
  );
}
