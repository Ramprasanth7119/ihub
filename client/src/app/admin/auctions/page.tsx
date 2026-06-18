"use client";

import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
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
import { Search, Plus, Play, Square, XCircle, Edit } from "lucide-react";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { toast } from "sonner";
import { format } from "date-fns";

const STATUS_COLORS: Record<string, string> = {
  UPCOMING: "bg-blue-100 text-blue-800",
  ACTIVE: "bg-green-100 text-green-800",
  CLOSED: "bg-gray-100 text-gray-800",
  CANCELLED: "bg-red-100 text-red-800",
  SCHEDULED: "bg-yellow-100 text-yellow-800",
};

export default function AdminAuctionsPage() {
  const [page, setPage] = useState(0);
  const [status, setStatus] = useState<string>("");
  const [showCreateDialog, setShowCreateDialog] = useState(false);
  const queryClient = useQueryClient();

  const { data: auctions, isLoading } = useQuery({
    queryKey: ["admin", "auctions", page, status],
    queryFn: () => adminService.getAuctions({ page, size: 20, status: status || undefined }),
  });

  const startMutation = useMutation({
    mutationFn: (id: number) => adminService.startAuction(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin", "auctions"] });
      toast.success("Auction started successfully");
    },
    onError: () => {
      toast.error("Failed to start auction");
    },
  });

  const endMutation = useMutation({
    mutationFn: (id: number) => adminService.endAuction(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin", "auctions"] });
      toast.success("Auction ended successfully");
    },
    onError: () => {
      toast.error("Failed to end auction");
    },
  });

  const cancelMutation = useMutation({
    mutationFn: (id: number) => adminService.cancelAuction(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin", "auctions"] });
      toast.success("Auction cancelled successfully");
    },
    onError: () => {
      toast.error("Failed to cancel auction");
    },
  });

  const handleStart = (id: number) => {
    if (confirm("Are you sure you want to start this auction?")) {
      startMutation.mutate(id);
    }
  };

  const handleEnd = (id: number) => {
    if (confirm("Are you sure you want to end this auction?")) {
      endMutation.mutate(id);
    }
  };

  const handleCancel = (id: number) => {
    if (confirm("Are you sure you want to cancel this auction?")) {
      cancelMutation.mutate(id);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Auctions</h1>
          <p className="mt-2 text-gray-600">Manage and control all auctions</p>
        </div>
        <Button onClick={() => setShowCreateDialog(true)}>
          <Plus className="mr-2 h-4 w-4" />
          Create Auction
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
                <Input placeholder="Search auctions..." className="pl-10" />
              </div>
            </div>
            <Select value={status} onValueChange={setStatus}>
              <SelectTrigger className="w-[180px]">
                <SelectValue placeholder="Status" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="">All Status</SelectItem>
                <SelectItem value="UPCOMING">Upcoming</SelectItem>
                <SelectItem value="ACTIVE">Active</SelectItem>
                <SelectItem value="CLOSED">Closed</SelectItem>
                <SelectItem value="CANCELLED">Cancelled</SelectItem>
                <SelectItem value="SCHEDULED">Scheduled</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardContent className="p-0">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Idea</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Start Time</TableHead>
                <TableHead>End Time</TableHead>
                <TableHead>Bids</TableHead>
                <TableHead>Highest Bid</TableHead>
                <TableHead>Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {isLoading ? (
                <TableRow>
                  <TableCell colSpan={7} className="text-center py-8">
                    Loading...
                  </TableCell>
                </TableRow>
              ) : auctions?.content.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={7} className="text-center py-8">
                    No auctions found
                  </TableCell>
                </TableRow>
              ) : (
                auctions?.content.map((auction) => (
                  <TableRow key={auction.id}>
                    <TableCell className="font-medium">{auction.ideaTitle}</TableCell>
                    <TableCell>
                      <Badge className={STATUS_COLORS[auction.status] || "bg-gray-100 text-gray-800"}>
                        {auction.status}
                      </Badge>
                    </TableCell>
                    <TableCell>{format(new Date(auction.startTime), "MMM dd, yyyy HH:mm")}</TableCell>
                    <TableCell>{format(new Date(auction.endTime), "MMM dd, yyyy HH:mm")}</TableCell>
                    <TableCell>{auction.bidCount}</TableCell>
                    <TableCell>${auction.highestBid?.toLocaleString() || "0"}</TableCell>
                    <TableCell>
                      <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                          <Button variant="ghost" size="icon">
                            <Edit className="h-4 w-4" />
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end">
                          {(auction.status === "UPCOMING" || auction.status === "SCHEDULED") && (
                            <DropdownMenuItem onClick={() => handleStart(auction.id)}>
                              <Play className="mr-2 h-4 w-4" />
                              Start Auction
                            </DropdownMenuItem>
                          )}
                          {auction.status === "ACTIVE" && (
                            <DropdownMenuItem onClick={() => handleEnd(auction.id)}>
                              <Square className="mr-2 h-4 w-4" />
                              End Auction
                            </DropdownMenuItem>
                          )}
                          {(auction.status === "UPCOMING" || auction.status === "SCHEDULED" || auction.status === "ACTIVE") && (
                            <DropdownMenuItem onClick={() => handleCancel(auction.id)} className="text-red-600">
                              <XCircle className="mr-2 h-4 w-4" />
                              Cancel Auction
                            </DropdownMenuItem>
                          )}
                        </DropdownMenuContent>
                      </DropdownMenu>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </CardContent>
      </Card>

      {auctions && auctions.totalElements > 20 && (
        <div className="flex items-center justify-between">
          <p className="text-sm text-gray-600">
            Showing {page * 20 + 1} to {Math.min((page + 1) * 20, auctions.totalElements)} of {auctions.totalElements} auctions
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
              disabled={(page + 1) * 20 >= auctions.totalElements}
            >
              Next
            </Button>
          </div>
        </div>
      )}

      {showCreateDialog && (
        <CreateAuctionDialog
          onClose={() => setShowCreateDialog(false)}
          onSuccess={() => {
            setShowCreateDialog(false);
            queryClient.invalidateQueries({ queryKey: ["admin", "auctions"] });
          }}
        />
      )}
    </div>
  );
}

function CreateAuctionDialog({ onClose, onSuccess }: { onClose: () => void; onSuccess: () => void }) {
  const [ideaId, setIdeaId] = useState("");
  const [startTime, setStartTime] = useState("");
  const [endTime, setEndTime] = useState("");
  const [minBid, setMinBid] = useState("");
  const [reservePrice, setReservePrice] = useState("");
  const [description, setDescription] = useState("");
  const queryClient = useQueryClient();

  const createMutation = useMutation({
    mutationFn: (data: any) => adminService.createAuction(data),
    onSuccess: () => {
      toast.success("Auction created successfully");
      onSuccess();
    },
    onError: () => {
      toast.error("Failed to create auction");
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    createMutation.mutate({
      ideaId: parseInt(ideaId),
      startTime,
      endTime,
      minBid: minBid ? parseFloat(minBid) : undefined,
      reservePrice: reservePrice ? parseFloat(reservePrice) : undefined,
      description,
    });
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
      <Card className="w-full max-w-lg">
        <CardHeader>
          <CardTitle>Create Auction</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium mb-1">Idea ID</label>
              <Input value={ideaId} onChange={(e) => setIdeaId(e.target.value)} required />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Start Time</label>
              <Input
                type="datetime-local"
                value={startTime}
                onChange={(e) => setStartTime(e.target.value)}
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">End Time</label>
              <Input
                type="datetime-local"
                value={endTime}
                onChange={(e) => setEndTime(e.target.value)}
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Minimum Bid</label>
              <Input
                type="number"
                step="0.01"
                value={minBid}
                onChange={(e) => setMinBid(e.target.value)}
              />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Reserve Price</label>
              <Input
                type="number"
                step="0.01"
                value={reservePrice}
                onChange={(e) => setReservePrice(e.target.value)}
              />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">Description</label>
              <textarea
                className="w-full border rounded-md p-2"
                rows={3}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
              />
            </div>
            <div className="flex gap-2 justify-end">
              <Button type="button" variant="outline" onClick={onClose}>
                Cancel
              </Button>
              <Button type="submit" disabled={createMutation.isPending}>
                {createMutation.isPending ? "Creating..." : "Create Auction"}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
