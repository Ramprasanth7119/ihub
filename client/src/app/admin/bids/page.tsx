"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { adminService } from "@/services/admin.service";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Badge } from "@/components/ui/badge";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Search, Download } from "lucide-react";
import { format } from "date-fns";

const STATUS_COLORS: Record<string, string> = {
  ACTIVE: "bg-green-100 text-green-800",
  CLOSED: "bg-gray-100 text-gray-800",
  CANCELLED: "bg-red-100 text-red-800",
  UPCOMING: "bg-blue-100 text-blue-800",
  SCHEDULED: "bg-yellow-100 text-yellow-800",
};

export default function AdminBidsPage() {
  const [page, setPage] = useState(0);
  const [auctionId, setAuctionId] = useState<string>("");
  const [investorId, setInvestorId] = useState<string>("");
  const [search, setSearch] = useState("");

  const { data: bids, isLoading } = useQuery({
    queryKey: ["admin", "bids", page, auctionId, investorId],
    queryFn: () =>
      adminService.getBids({
        page,
        size: 20,
        auctionId: auctionId ? parseInt(auctionId) : undefined,
        investorId: investorId ? parseInt(investorId) : undefined,
      }),
  });

  const filteredBids = bids?.content.filter(
    (bid) =>
      bid.ideaTitle.toLowerCase().includes(search.toLowerCase()) ||
      bid.investorName.toLowerCase().includes(search.toLowerCase()) ||
      bid.investorEmail.toLowerCase().includes(search.toLowerCase())
  ) || [];

  const handleExport = () => {
    const csvContent =
      "data:text/csv;charset=utf-8," +
      "ID,Auction,Idea,Investor,Investor Email,Amount,Rank,Time\n" +
      filteredBids
        .map(
          (bid) =>
            `${bid.id},${bid.auctionId},${bid.ideaTitle},${bid.investorName},${bid.investorEmail},${bid.bidAmount},${bid.rank},${format(new Date(bid.createdAt), "yyyy-MM-dd HH:mm:ss")}`
        )
        .join("\n");

    const link = document.createElement("a");
    link.setAttribute("href", encodeURI(csvContent));
    link.setAttribute("download", "bids_export.csv");
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Bids</h1>
          <p className="mt-2 text-gray-600">Monitor and manage all bidding activity</p>
        </div>
        <Button onClick={handleExport}>
          <Download className="mr-2 h-4 w-4" />
          Export CSV
        </Button>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Filters</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex gap-4">
            <div className="flex-1">
              <div className="relative">
                <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
                <Input
                  placeholder="Search bids..."
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  className="pl-10"
                />
              </div>
            </div>
            <Input
              placeholder="Auction ID"
              value={auctionId}
              onChange={(e) => setAuctionId(e.target.value)}
              className="w-[150px]"
            />
            <Input
              placeholder="Investor ID"
              value={investorId}
              onChange={(e) => setInvestorId(e.target.value)}
              className="w-[150px]"
            />
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardContent className="p-0">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Idea</TableHead>
                <TableHead>Investor</TableHead>
                <TableHead>Amount</TableHead>
                <TableHead>Rank</TableHead>
                <TableHead>Auction Status</TableHead>
                <TableHead>Time</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {isLoading ? (
                <TableRow>
                  <TableCell colSpan={6} className="text-center py-8">
                    Loading...
                  </TableCell>
                </TableRow>
              ) : filteredBids.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={6} className="text-center py-8">
                    No bids found
                  </TableCell>
                </TableRow>
              ) : (
                filteredBids.map((bid) => (
                  <TableRow key={bid.id}>
                    <TableCell className="font-medium">{bid.ideaTitle}</TableCell>
                    <TableCell>
                      <div>
                        <p className="font-medium">{bid.investorName}</p>
                        <p className="text-sm text-gray-500">{bid.investorEmail}</p>
                      </div>
                    </TableCell>
                    <TableCell className="font-bold">${bid.bidAmount.toLocaleString()}</TableCell>
                    <TableCell>
                      <Badge variant={bid.rank === 1 ? "success" : "default"}>#{bid.rank}</Badge>
                    </TableCell>
                    <TableCell>
                      <Badge className={STATUS_COLORS[bid.auctionStatus] || "bg-gray-100 text-gray-800"}>
                        {bid.auctionStatus}
                      </Badge>
                    </TableCell>
                    <TableCell>{format(new Date(bid.createdAt), "MMM dd, yyyy HH:mm")}</TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </CardContent>
      </Card>

      {bids && bids.totalElements > 20 && (
        <div className="flex items-center justify-between">
          <p className="text-sm text-gray-600">
            Showing {page * 20 + 1} to {Math.min((page + 1) * 20, bids.totalElements)} of {bids.totalElements} bids
          </p>
          <div className="flex gap-2">
            <Button
              variant="outline"
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={page === 0}
            >
              Previous
            </Button>
            <Button
              variant="outline"
              onClick={() => setPage((p) => p + 1)}
              disabled={(page + 1) * 20 >= bids.totalElements}
            >
              Next
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
