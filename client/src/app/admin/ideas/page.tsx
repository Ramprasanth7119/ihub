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
import { Search, MoreHorizontal, Check, X, Trash2 } from "lucide-react";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { toast } from "sonner";

const STATUS_COLORS: Record<string, string> = {
  PENDING: "bg-yellow-100 text-yellow-800",
  APPROVED: "bg-green-100 text-green-800",
  REJECTED: "bg-red-100 text-red-800",
  SUSPENDED: "bg-orange-100 text-orange-800",
  DRAFT: "bg-gray-100 text-gray-800",
  PUBLISHED: "bg-blue-100 text-blue-800",
  ARCHIVED: "bg-gray-200 text-gray-600",
};

export default function AdminIdeasPage() {
  const [page, setPage] = useState(0);
  const [status, setStatus] = useState<string>("");
  const [category, setCategory] = useState<string>("");
  const [search, setSearch] = useState("");
  const queryClient = useQueryClient();

  const { data: ideas, isLoading } = useQuery({
    queryKey: ["admin", "ideas", page, status, category],
    queryFn: () => adminService.getIdeas({ page, size: 20, status: status || undefined, category: category || undefined }),
  });

  const updateStatusMutation = useMutation({
    mutationFn: ({ id, status, reason }: { id: number; status: string; reason?: string }) =>
      adminService.updateIdeaStatus(id, status, reason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin", "ideas"] });
      toast.success("Idea status updated successfully");
    },
    onError: () => {
      toast.error("Failed to update idea status");
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => adminService.deleteIdea(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin", "ideas"] });
      toast.success("Idea deleted successfully");
    },
    onError: () => {
      toast.error("Failed to delete idea");
    },
  });

  const handleApprove = (id: number) => {
    updateStatusMutation.mutate({ id, status: "APPROVED", reason: "Approved by admin" });
  };

  const handleReject = (id: number) => {
    updateStatusMutation.mutate({ id, status: "REJECTED", reason: "Rejected by admin" });
  };

  const handleSuspend = (id: number) => {
    updateStatusMutation.mutate({ id, status: "SUSPENDED", reason: "Suspended by admin" });
  };

  const handleDelete = (id: number) => {
    if (confirm("Are you sure you want to delete this idea?")) {
      deleteMutation.mutate(id);
    }
  };

  const filteredIdeas = ideas?.content.filter(
    (idea) =>
      idea.title.toLowerCase().includes(search.toLowerCase()) ||
      idea.creatorName.toLowerCase().includes(search.toLowerCase())
  ) || [];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Ideas</h1>
          <p className="mt-2 text-gray-600">Manage and review submitted ideas</p>
        </div>
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
                  placeholder="Search ideas..."
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  className="pl-10"
                />
              </div>
            </div>
            <Select value={status} onValueChange={setStatus}>
              <SelectTrigger className="w-[180px]">
                <SelectValue placeholder="Status" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="">All Status</SelectItem>
                <SelectItem value="PENDING">Pending</SelectItem>
                <SelectItem value="APPROVED">Approved</SelectItem>
                <SelectItem value="REJECTED">Rejected</SelectItem>
                <SelectItem value="SUSPENDED">Suspended</SelectItem>
                <SelectItem value="DRAFT">Draft</SelectItem>
                <SelectItem value="PUBLISHED">Published</SelectItem>
              </SelectContent>
            </Select>
            <Select value={category} onValueChange={setCategory}>
              <SelectTrigger className="w-[180px]">
                <SelectValue placeholder="Category" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="">All Categories</SelectItem>
                <SelectItem value="Technology">Technology</SelectItem>
                <SelectItem value="Healthcare">Healthcare</SelectItem>
                <SelectItem value="Finance">Finance</SelectItem>
                <SelectItem value="Education">Education</SelectItem>
                <SelectItem value="Entertainment">Entertainment</SelectItem>
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
                <TableHead>Title</TableHead>
                <TableHead>Creator</TableHead>
                <TableHead>Category</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Base Price</TableHead>
                <TableHead>Auctions</TableHead>
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
              ) : filteredIdeas.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={7} className="text-center py-8">
                    No ideas found
                  </TableCell>
                </TableRow>
              ) : (
                filteredIdeas.map((idea) => (
                  <TableRow key={idea.id}>
                    <TableCell className="font-medium">{idea.title}</TableCell>
                    <TableCell>
                      <div>
                        <p className="font-medium">{idea.creatorName}</p>
                        <p className="text-sm text-gray-500">{idea.creatorEmail}</p>
                      </div>
                    </TableCell>
                    <TableCell>{idea.category}</TableCell>
                    <TableCell>
                      <Badge className={STATUS_COLORS[idea.status] || "bg-gray-100 text-gray-800"}>
                        {idea.status}
                      </Badge>
                    </TableCell>
                    <TableCell>${idea.basePrice?.toLocaleString()}</TableCell>
                    <TableCell>{idea.auctionCount}</TableCell>
                    <TableCell>
                      <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                          <Button variant="ghost" size="icon">
                            <MoreHorizontal className="h-4 w-4" />
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end">
                          {idea.status === "PENDING" && (
                            <>
                              <DropdownMenuItem onClick={() => handleApprove(idea.id)}>
                                <Check className="mr-2 h-4 w-4" />
                                Approve
                              </DropdownMenuItem>
                              <DropdownMenuItem onClick={() => handleReject(idea.id)}>
                                <X className="mr-2 h-4 w-4" />
                                Reject
                              </DropdownMenuItem>
                            </>
                          )}
                          {idea.status === "APPROVED" && (
                            <DropdownMenuItem onClick={() => handleSuspend(idea.id)}>
                              Suspend
                            </DropdownMenuItem>
                          )}
                          <DropdownMenuItem onClick={() => handleDelete(idea.id)} className="text-red-600">
                            <Trash2 className="mr-2 h-4 w-4" />
                            Delete
                          </DropdownMenuItem>
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

      {ideas && ideas.totalElements > 20 && (
        <div className="flex items-center justify-between">
          <p className="text-sm text-gray-600">
            Showing {page * 20 + 1} to {Math.min((page + 1) * 20, ideas.totalElements)} of {ideas.totalElements} ideas
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
              disabled={(page + 1) * 20 >= ideas.totalElements}
            >
              Next
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
