"use client";

import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { adminService } from "@/services/admin.service";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { ErrorState } from "@/components/shared/error-state";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Trophy, Search, Download, ChevronLeft, ChevronRight } from "lucide-react";
import { format } from "date-fns";
import { formatCurrency } from "@/lib/utils";

const PAGE_SIZE = 20;

/**
 * Decided auctions and their winners.
 *
 * <p>Backed by a single `/admin/winners` query that joins auction, idea, creator and
 * winner server-side. The previous version listed closed auctions and then issued a
 * winner lookup per row, so a page of 20 cost 41 requests.</p>
 */
export default function AdminWinnersPage() {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");

  const { data, isLoading, isError, error, refetch, isFetching } = useQuery({
    queryKey: ["admin", "winners", page],
    queryFn: () => adminService.getWinners({ page, size: PAGE_SIZE }),
    placeholderData: (previous) => previous,
  });

  // The `?? []` fallback lives inside useMemo: written outside, it allocates a new
  // array on every render and defeats the memo entirely.
  const winners = useMemo(() => data?.content ?? [], [data]);

  // Filtering is client-side over the current page only; the label below says so
  // rather than implying it searches every winner on the platform.
  const visible = useMemo(() => {
    const term = search.trim().toLowerCase();
    if (!term) return winners;
    return winners.filter(
      (winner) =>
        winner.ideaTitle.toLowerCase().includes(term) ||
        winner.winnerName.toLowerCase().includes(term) ||
        winner.creatorName.toLowerCase().includes(term)
    );
  }, [winners, search]);

  const totalValue = winners.reduce((sum, winner) => sum + winner.winningBid, 0);
  const totalPages = Math.max(1, Math.ceil((data?.totalElements ?? 0) / PAGE_SIZE));

  const handleExport = () => {
    const header = [
      "Auction ID",
      "Idea",
      "Category",
      "Creator",
      "Winner",
      "Winner Email",
      "Winning Bid",
      "Bids",
      "Decided At",
    ];
    const rows = visible.map((winner) => [
      winner.auctionId,
      winner.ideaTitle,
      winner.ideaCategory ?? "",
      winner.creatorName,
      winner.winnerName,
      winner.winnerEmail,
      winner.winningBid,
      winner.bidCount,
      winner.decidedAt,
    ]);

    const csv = [header, ...rows]
      .map((row) => row.map((cell) => `"${String(cell ?? "").replace(/"/g, '""')}"`).join(","))
      .join("\n");

    const blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = `ihub-winners-page-${page + 1}.csv`;
    link.click();
    URL.revokeObjectURL(url);
  };

  if (isError) {
    return (
      <div className="space-y-6">
        <PageTitle />
        <ErrorState error={error} onRetry={() => refetch()} />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <PageTitle />
        <Button variant="outline" onClick={handleExport} disabled={visible.length === 0}>
          <Download className="mr-2 h-4 w-4" />
          Export CSV
        </Button>
      </div>

      <div className="grid gap-4 sm:grid-cols-3">
        <SummaryTile label="Decided auctions" value={(data?.totalElements ?? 0).toLocaleString()} />
        <SummaryTile label="Value on this page" value={formatCurrency(totalValue)} />
        <SummaryTile
          label="Average winning bid"
          value={winners.length ? formatCurrency(totalValue / winners.length) : "—"}
        />
      </div>

      <Card>
        <CardContent className="space-y-4 p-4 sm:p-6">
          <div className="relative max-w-sm">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
            <Input
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Filter this page…"
              className="pl-9"
              aria-label="Filter winners on this page"
            />
          </div>

          <div className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Auction</TableHead>
                  <TableHead>Idea</TableHead>
                  <TableHead>Creator</TableHead>
                  <TableHead>Winner</TableHead>
                  <TableHead className="text-right">Winning bid</TableHead>
                  <TableHead className="text-right">Bids</TableHead>
                  <TableHead>Decided</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {isLoading ? (
                  Array.from({ length: 5 }).map((_, index) => (
                    <TableRow key={index}>
                      <TableCell colSpan={7}>
                        <Skeleton className="h-8 w-full" />
                      </TableCell>
                    </TableRow>
                  ))
                ) : visible.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={7} className="py-12 text-center">
                      <Trophy className="mx-auto mb-3 h-8 w-8 text-gray-300" />
                      <p className="font-medium text-gray-700">
                        {search ? "No winners match that filter" : "No auctions have been decided yet"}
                      </p>
                      <p className="mt-1 text-sm text-gray-500">
                        {search
                          ? "Try a different name or idea title."
                          : "Winners appear here once an auction closes with at least one bid."}
                      </p>
                    </TableCell>
                  </TableRow>
                ) : (
                  visible.map((winner) => (
                    <TableRow key={winner.auctionId}>
                      <TableCell className="font-medium">#{winner.auctionId}</TableCell>
                      <TableCell>
                        <div className="max-w-[220px]">
                          <p className="truncate font-medium text-gray-900">{winner.ideaTitle}</p>
                          {winner.ideaCategory && (
                            <Badge variant="secondary" className="mt-1 capitalize">
                              {winner.ideaCategory}
                            </Badge>
                          )}
                        </div>
                      </TableCell>
                      <TableCell className="text-gray-600">{winner.creatorName}</TableCell>
                      <TableCell>
                        <div className="flex items-center gap-2">
                          <Trophy className="h-4 w-4 shrink-0 text-amber-500" />
                          <div className="min-w-0">
                            <p className="truncate font-medium text-gray-900">{winner.winnerName}</p>
                            <p className="truncate text-xs text-gray-500">{winner.winnerEmail}</p>
                          </div>
                        </div>
                      </TableCell>
                      <TableCell className="text-right font-semibold text-emerald-700">
                        {formatCurrency(winner.winningBid)}
                      </TableCell>
                      <TableCell className="text-right text-gray-600">{winner.bidCount}</TableCell>
                      <TableCell className="whitespace-nowrap text-sm text-gray-600">
                        {winner.decidedAt
                          ? format(new Date(winner.decidedAt), "dd MMM yyyy, HH:mm")
                          : "—"}
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </div>

          <div className="flex items-center justify-between border-t border-gray-100 pt-4">
            <p className="text-sm text-gray-600">
              Page {page + 1} of {totalPages}
              {search && ` · filtering ${visible.length} of ${winners.length} on this page`}
            </p>
            <div className="flex gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0 || isFetching}
              >
                <ChevronLeft className="h-4 w-4" />
                Previous
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setPage((p) => p + 1)}
                disabled={page + 1 >= totalPages || isFetching}
              >
                Next
                <ChevronRight className="h-4 w-4" />
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

function PageTitle() {
  return (
    <div>
      <h1 className="text-3xl font-bold text-gray-900">Winners</h1>
      <p className="mt-2 text-gray-600">Every decided auction, its winner and the winning bid.</p>
    </div>
  );
}

function SummaryTile({ label, value }: { label: string; value: string }) {
  return (
    <Card>
      <CardContent className="p-5">
        <p className="text-sm text-gray-500">{label}</p>
        <p className="mt-1 text-2xl font-bold text-gray-900">{value}</p>
      </CardContent>
    </Card>
  );
}
