"use client";

import { useQuery } from "@tanstack/react-query";
import { auctionService } from "@/services/auction.service";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Trophy, DollarSign } from "lucide-react";
import { format } from "date-fns";

export default function AdminWinnersPage() {
  const { data: auctions, isLoading } = useQuery({
    queryKey: ["admin", "auctions-winners"],
    queryFn: () => auctionService.getAll("CLOSED"),
  });

  const winnersData = auctions?.map(async (auction) => {
    try {
      const winner = await auctionService.getWinner(auction.id);
      return { auction, winner };
    } catch {
      return { auction, winner: null };
    }
  }) || [];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold text-gray-900">Winners</h1>
        <p className="mt-2 text-gray-600">View auction winners and winning bids</p>
      </div>

      <Card>
        <CardContent className="p-0">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Auction</TableHead>
                <TableHead>Idea</TableHead>
                <TableHead>Winner</TableHead>
                <TableHead>Winning Bid</TableHead>
                <TableHead>Closed At</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {isLoading ? (
                <TableRow>
                  <TableCell colSpan={5} className="text-center py-8">
                    Loading...
                  </TableCell>
                </TableRow>
              ) : !auctions || auctions.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={5} className="text-center py-8">
                    No closed auctions found
                  </TableCell>
                </TableRow>
              ) : (
                auctions.map((auction) => (
                  <WinnerRow key={auction.id} auctionId={auction.id} />
                ))
              )}
            </TableBody>
          </Table>
        </CardContent>
      </Card>
    </div>
  );
}

function WinnerRow({ auctionId }: { auctionId: number }) {
  const { data: winner, isLoading } = useQuery({
    queryKey: ["auction", auctionId, "winner"],
    queryFn: () => auctionService.getWinner(auctionId),
  });

  const { data: auction } = useQuery({
    queryKey: ["auction", auctionId],
    queryFn: () => auctionService.getById(auctionId),
  });

  if (isLoading) {
    return (
      <TableRow>
        <TableCell colSpan={5} className="text-center py-4">
          Loading...
        </TableCell>
      </TableRow>
    );
  }

  return (
    <TableRow>
      <TableCell className="font-medium">#{auctionId}</TableCell>
      <TableCell>{auction?.ideaId || "—"}</TableCell>
      <TableCell>
        {winner ? (
          <div className="flex items-center gap-2">
            <Trophy className="h-4 w-4 text-yellow-500" />
            <span className="font-medium">{winner.winnerName}</span>
          </div>
        ) : (
          <span className="text-gray-400">No winner</span>
        )}
      </TableCell>
      <TableCell>
        {winner ? (
          <div className="flex items-center gap-2">
            <DollarSign className="h-4 w-4 text-green-600" />
            <span className="font-bold">${winner.winningBid.toLocaleString()}</span>
          </div>
        ) : (
          <span className="text-gray-400">—</span>
        )}
      </TableCell>
      <TableCell>
        {winner ? format(new Date(winner.closedAt), "MMM dd, yyyy HH:mm") : "—"}
      </TableCell>
    </TableRow>
  );
}
