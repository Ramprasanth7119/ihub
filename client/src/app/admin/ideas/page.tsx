"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { adminService } from "@/services/admin.service";
import { categoryService } from "@/services/category.service";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { ErrorState } from "@/components/shared/error-state";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  Search,
  MoreHorizontal,
  Check,
  X,
  Trash2,
  Eye,
  Ban,
  ChevronLeft,
  ChevronRight,
} from "lucide-react";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { toast } from "sonner";
import { getErrorMessage } from "@/lib/api-error";
import { cn, formatCurrency, getAdminStatusColor } from "@/lib/utils";
import type { AdminIdea } from "@/types";

const PAGE_SIZE = 20;

const STATUS_FILTERS = [
  { value: "", label: "All" },
  { value: "PUBLISHED", label: "Published" },
  { value: "DRAFT", label: "Draft" },
  { value: "APPROVED", label: "Approved" },
  { value: "SUSPENDED", label: "Suspended" },
  { value: "REJECTED", label: "Rejected" },
] as const;

/** Moderation decisions that require an explanation sent to the creator. */
const REASONED_STATUSES = new Set(["REJECTED", "SUSPENDED"]);

export default function AdminIdeasPage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [status, setStatus] = useState("");
  const [category, setCategory] = useState("");
  const [search, setSearch] = useState("");
  const [moderating, setModerating] = useState<{ idea: AdminIdea; status: string } | null>(null);
  const [deleting, setDeleting] = useState<AdminIdea | null>(null);

  const { data, isLoading, isError, error, refetch, isFetching } = useQuery({
    queryKey: ["admin", "ideas", page, status, category],
    queryFn: () =>
      adminService.getIdeas({
        page,
        size: PAGE_SIZE,
        status: status || undefined,
        category: category || undefined,
      }),
    placeholderData: (previous) => previous,
  });

  // Categories come from the API. They were previously a hardcoded, capitalised
  // list ("Technology"), while ideas store the lowercase slug ("technology") — so
  // the filter matched nothing and always returned an empty table.
  const { data: categories = [] } = useQuery({
    queryKey: ["categories"],
    queryFn: () => categoryService.getAll(),
    staleTime: 10 * 60 * 1000,
  });

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ["admin", "ideas"] });
    queryClient.invalidateQueries({ queryKey: ["admin", "metrics"] });
  };

  const moderateMutation = useMutation({
    mutationFn: ({ id, status: next, reason }: { id: number; status: string; reason?: string }) =>
      adminService.updateIdeaStatus(id, next, reason),
    onSuccess: () => {
      invalidate();
      setModerating(null);
      toast.success("Idea updated and the creator has been notified");
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => adminService.deleteIdea(id),
    onSuccess: () => {
      invalidate();
      setDeleting(null);
      toast.success("Idea deleted");
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  // Memoised so the filter useMemo below isn't invalidated on every render.
  const ideas = useMemo(() => data?.content ?? [], [data]);
  const totalPages = Math.max(1, Math.ceil((data?.totalElements ?? 0) / PAGE_SIZE));

  const visible = useMemo(() => {
    const term = search.trim().toLowerCase();
    if (!term) return ideas;
    return ideas.filter(
      (idea) =>
        idea.title.toLowerCase().includes(term) ||
        idea.creatorName.toLowerCase().includes(term) ||
        idea.creatorEmail?.toLowerCase().includes(term)
    );
  }, [ideas, search]);

  const applyModeration = (idea: AdminIdea, next: string) => {
    if (REASONED_STATUSES.has(next)) {
      // Rejections and suspensions reach the creator as a notification, so a
      // specific reason is collected instead of a canned "Rejected by admin".
      setModerating({ idea, status: next });
      return;
    }
    moderateMutation.mutate({ id: idea.id, status: next });
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold text-gray-900">Ideas</h1>
        <p className="mt-2 text-gray-600">
          Review submitted ideas and control what appears in investor discovery.
        </p>
      </div>

      <Card>
        <CardContent className="space-y-4 p-4 sm:p-6">
          <div className="flex flex-col gap-3 lg:flex-row lg:items-center">
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
              <Input
                placeholder="Filter this page by title or creator…"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="pl-10"
                aria-label="Filter ideas on this page"
              />
            </div>

            <select
              value={category}
              onChange={(e) => {
                setCategory(e.target.value);
                setPage(0);
              }}
              aria-label="Filter by category"
              className="rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 focus:border-violet-500 focus:outline-none focus:ring-1 focus:ring-violet-500"
            >
              <option value="">All categories</option>
              {categories.map((cat) => (
                <option key={cat.id} value={cat.slug}>
                  {cat.name}
                </option>
              ))}
            </select>
          </div>

          <div className="flex flex-wrap gap-1 rounded-lg bg-gray-100 p-1">
            {STATUS_FILTERS.map((filter) => (
              <button
                key={filter.value || "all"}
                type="button"
                onClick={() => {
                  setStatus(filter.value);
                  setPage(0);
                }}
                aria-pressed={status === filter.value}
                className={cn(
                  "rounded-md px-3 py-1.5 text-sm font-medium transition-colors",
                  status === filter.value
                    ? "bg-white text-gray-900 shadow-sm"
                    : "text-gray-600 hover:text-gray-900"
                )}
              >
                {filter.label}
              </button>
            ))}
          </div>

          {isError ? (
            <ErrorState error={error} onRetry={() => refetch()} />
          ) : (
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Idea</TableHead>
                    <TableHead>Creator</TableHead>
                    <TableHead>Category</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead className="text-right">Base price</TableHead>
                    <TableHead className="text-right">Auctions</TableHead>
                    <TableHead className="w-20 text-right">Actions</TableHead>
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
                        <p className="font-medium text-gray-700">No ideas found</p>
                        <p className="mt-1 text-sm text-gray-500">
                          Try clearing the status or category filter.
                        </p>
                      </TableCell>
                    </TableRow>
                  ) : (
                    visible.map((idea) => (
                      <TableRow key={idea.id} className="hover:bg-gray-50">
                        <TableCell>
                          <p className="max-w-[240px] truncate font-medium text-gray-900">
                            {idea.title}
                          </p>
                          <p className="text-xs text-gray-500">#{idea.id}</p>
                        </TableCell>
                        <TableCell>
                          <p className="font-medium text-gray-900">{idea.creatorName}</p>
                          <p className="text-sm text-gray-500">{idea.creatorEmail}</p>
                        </TableCell>
                        <TableCell className="capitalize text-gray-600">{idea.category}</TableCell>
                        <TableCell>
                          <Badge variant="outline" className={getAdminStatusColor(idea.status)}>
                            {idea.status}
                          </Badge>
                        </TableCell>
                        <TableCell className="text-right">
                          {idea.basePrice ? formatCurrency(idea.basePrice) : "—"}
                        </TableCell>
                        <TableCell className="text-right">
                          {idea.auctionCount}
                          {idea.hasActiveAuction && (
                            <span className="ml-1 text-xs text-emerald-600">live</span>
                          )}
                        </TableCell>
                        <TableCell className="text-right">
                          <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                              <Button variant="ghost" size="icon" aria-label="Idea actions">
                                <MoreHorizontal className="h-4 w-4" />
                              </Button>
                            </DropdownMenuTrigger>
                            <DropdownMenuContent align="end">
                              <DropdownMenuItem asChild>
                                <Link href={`/ideas/${idea.id}`}>
                                  <Eye className="mr-2 h-4 w-4" />
                                  View idea
                                </Link>
                              </DropdownMenuItem>
                              {idea.status !== "APPROVED" && idea.status !== "PUBLISHED" && (
                                <DropdownMenuItem onClick={() => applyModeration(idea, "APPROVED")}>
                                  <Check className="mr-2 h-4 w-4" />
                                  Approve
                                </DropdownMenuItem>
                              )}
                              {idea.status !== "REJECTED" && (
                                <DropdownMenuItem onClick={() => applyModeration(idea, "REJECTED")}>
                                  <X className="mr-2 h-4 w-4" />
                                  Reject
                                </DropdownMenuItem>
                              )}
                              {idea.status !== "SUSPENDED" && (
                                <DropdownMenuItem onClick={() => applyModeration(idea, "SUSPENDED")}>
                                  <Ban className="mr-2 h-4 w-4" />
                                  Suspend
                                </DropdownMenuItem>
                              )}
                              <DropdownMenuItem
                                onClick={() => setDeleting(idea)}
                                className="text-red-600"
                              >
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
            </div>
          )}

          <div className="flex items-center justify-between border-t border-gray-100 pt-4">
            <p className="text-sm text-gray-600">
              Page {page + 1} of {totalPages} · {(data?.totalElements ?? 0).toLocaleString()} ideas
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

      <ModerationDialog
        pending={moderating}
        onOpenChange={(open) => !open && setModerating(null)}
        loading={moderateMutation.isPending}
        onSubmit={(reason) =>
          moderating &&
          moderateMutation.mutate({ id: moderating.idea.id, status: moderating.status, reason })
        }
      />

      <ConfirmDialog
        open={deleting !== null}
        onOpenChange={(open) => !open && setDeleting(null)}
        title="Delete this idea?"
        description={`"${deleting?.title}" will be permanently removed.`}
        detail={
          deleting?.auctionCount
            ? `This idea has ${deleting.auctionCount} auction(s) associated with it. Deleting it may fail if auction records still reference it — suspending is usually the safer action.`
            : "This also removes it from investor search. This cannot be undone."
        }
        confirmLabel="Delete idea"
        destructive
        loading={deleteMutation.isPending}
        onConfirm={() => deleting && deleteMutation.mutate(deleting.id)}
      />
    </div>
  );
}

/** Collects the explanation that is delivered to the creator with the decision. */
function ModerationDialog({
  pending,
  onOpenChange,
  loading,
  onSubmit,
}: {
  pending: { idea: AdminIdea; status: string } | null;
  onOpenChange: (open: boolean) => void;
  loading: boolean;
  onSubmit: (reason: string) => void;
}) {
  const [reason, setReason] = useState("");
  const verb = pending?.status === "REJECTED" ? "Reject" : "Suspend";

  return (
    <Dialog
      open={pending !== null}
      onOpenChange={(open) => {
        if (!open) setReason("");
        onOpenChange(open);
      }}
    >
      <DialogContent>
        <DialogHeader>
          <DialogTitle>
            {verb} &ldquo;{pending?.idea.title}&rdquo;?
          </DialogTitle>
          <DialogDescription>
            The creator is notified of this decision, and the idea is removed from investor
            discovery.
          </DialogDescription>
        </DialogHeader>

        <div>
          <Label htmlFor="moderation-reason">Reason for the creator</Label>
          <Textarea
            id="moderation-reason"
            rows={3}
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            placeholder="Explain what needs to change, so the creator can act on it."
          />
          <p className="mt-1 text-xs text-gray-500">
            Included verbatim in the notification. Leave blank to send the decision without one.
          </p>
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)} disabled={loading}>
            Cancel
          </Button>
          <Button
            variant="destructive"
            onClick={() => onSubmit(reason.trim())}
            disabled={loading}
          >
            {loading ? "Working…" : `${verb} idea`}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
