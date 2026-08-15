"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { adminService } from "@/services/admin.service";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { ConfirmDialog } from "@/components/shared/confirm-dialog";
import { ErrorState } from "@/components/shared/error-state";
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
  Plus,
  Play,
  Square,
  XCircle,
  MoreHorizontal,
  Eye,
  ChevronLeft,
  ChevronRight,
  X,
} from "lucide-react";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { toast } from "sonner";
import { format } from "date-fns";
import { useDebounce } from "@/hooks/use-debounce";
import { getErrorMessage } from "@/lib/api-error";
import { cn, formatAuctionStatus, formatCurrency, getAdminStatusColor } from "@/lib/utils";
import type { AdminAuction, CreateAuctionPayload } from "@/types";

const PAGE_SIZE = 20;

const STATUS_FILTERS = [
  { value: "", label: "All statuses" },
  { value: "SCHEDULED", label: "Upcoming" },
  { value: "ACTIVE", label: "Live" },
  { value: "CLOSED", label: "Completed" },
  { value: "CANCELLED", label: "Cancelled" },
] as const;

type PendingAction = { type: "start" | "end" | "cancel"; auction: AdminAuction } | null;

export default function AdminAuctionsPage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [status, setStatus] = useState("");
  const [searchInput, setSearchInput] = useState("");
  const [showCreate, setShowCreate] = useState(false);
  const [pending, setPending] = useState<PendingAction>(null);

  // Debounced so typing doesn't fire a request per keystroke.
  const search = useDebounce(searchInput, 350);

  const { data, isLoading, isError, error, refetch, isFetching } = useQuery({
    queryKey: ["admin", "auctions", page, status, search],
    queryFn: () =>
      adminService.getAuctions({
        page,
        size: PAGE_SIZE,
        status: status || undefined,
        search: search.trim() || undefined,
      }),
    placeholderData: (previous) => previous,
  });

  // Memoised so the summary useMemo below isn't invalidated by a fresh array
  // literal on every render.
  const auctions = useMemo(() => data?.content ?? [], [data]);
  const totalPages = Math.max(1, Math.ceil((data?.totalElements ?? 0) / PAGE_SIZE));

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ["admin", "auctions"] });
    queryClient.invalidateQueries({ queryKey: ["admin", "metrics"] });
    queryClient.invalidateQueries({ queryKey: ["admin", "winners"] });
  };

  const actionMutation = useMutation({
    mutationFn: ({ type, auction }: NonNullable<PendingAction>) => {
      if (type === "start") return adminService.startAuction(auction.id);
      if (type === "end") return adminService.endAuction(auction.id);
      return adminService.cancelAuction(auction.id);
    },
    onSuccess: (_result, variables) => {
      invalidate();
      setPending(null);
      toast.success(SUCCESS_COPY[variables.type]);
    },
    onError: (err) => toast.error(getErrorMessage(err)),
  });

  const changeFilter = (next: string) => {
    setStatus(next);
    setPage(0);
  };

  const summary = useMemo(() => {
    const totalBids = auctions.reduce((sum, auction) => sum + auction.bidCount, 0);
    const totalValue = auctions.reduce((sum, auction) => sum + (auction.highestBid ?? 0), 0);
    return { totalBids, totalValue };
  }, [auctions]);

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Auctions</h1>
          <p className="mt-2 text-gray-600">
            Schedule auctions, drive their lifecycle and monitor live bidding.
          </p>
        </div>
        <Button onClick={() => setShowCreate(true)}>
          <Plus className="mr-2 h-4 w-4" />
          Create auction
        </Button>
      </div>

      <div className="grid gap-4 sm:grid-cols-3">
        <SummaryTile label="Matching auctions" value={(data?.totalElements ?? 0).toLocaleString()} />
        <SummaryTile label="Bids on this page" value={summary.totalBids.toLocaleString()} />
        <SummaryTile label="Top bid value on this page" value={formatCurrency(summary.totalValue)} />
      </div>

      <Card>
        <CardContent className="space-y-4 p-4 sm:p-6">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
              <Input
                value={searchInput}
                onChange={(e) => {
                  setSearchInput(e.target.value);
                  setPage(0);
                }}
                placeholder="Search by idea title or auction ID…"
                className="pl-10 pr-9"
                aria-label="Search auctions"
              />
              {searchInput && (
                <button
                  type="button"
                  onClick={() => setSearchInput("")}
                  className="absolute right-2 top-1/2 -translate-y-1/2 rounded p-1 text-gray-400 hover:text-gray-700"
                  aria-label="Clear search"
                >
                  <X className="h-4 w-4" />
                </button>
              )}
            </div>

            {/* Segmented control rather than a dropdown: five mutually exclusive
                states that admins switch between constantly. */}
            <div className="flex flex-wrap gap-1 rounded-lg bg-gray-100 p-1">
              {STATUS_FILTERS.map((filter) => (
                <button
                  key={filter.value || "all"}
                  type="button"
                  onClick={() => changeFilter(filter.value)}
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
          </div>

          {isError ? (
            <ErrorState error={error} onRetry={() => refetch()} />
          ) : (
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Idea</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead>Window</TableHead>
                    <TableHead className="text-right">Bids</TableHead>
                    <TableHead className="text-right">Highest</TableHead>
                    <TableHead className="w-24 text-right">Actions</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {isLoading ? (
                    Array.from({ length: 5 }).map((_, index) => (
                      <TableRow key={index}>
                        <TableCell colSpan={6}>
                          <Skeleton className="h-8 w-full" />
                        </TableCell>
                      </TableRow>
                    ))
                  ) : auctions.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={6} className="py-12 text-center">
                        <p className="font-medium text-gray-700">No auctions found</p>
                        <p className="mt-1 text-sm text-gray-500">
                          {search || status
                            ? "Try clearing the filters."
                            : "Create an auction to get started."}
                        </p>
                      </TableCell>
                    </TableRow>
                  ) : (
                    auctions.map((auction) => (
                      <TableRow key={auction.id} className="hover:bg-gray-50">
                        <TableCell>
                          <Link
                            href={`/admin/auctions/${auction.id}`}
                            className="font-medium text-gray-900 hover:text-violet-700 hover:underline"
                          >
                            {auction.ideaTitle}
                          </Link>
                          <p className="text-xs text-gray-500">Auction #{auction.id}</p>
                        </TableCell>
                        <TableCell>
                          <Badge
                            variant="outline"
                            className={getAdminStatusColor(auction.status)}
                          >
                            {formatAuctionStatus(auction.status)}
                          </Badge>
                        </TableCell>
                        <TableCell className="whitespace-nowrap text-sm text-gray-600">
                          <div>{format(new Date(auction.startTime), "dd MMM, HH:mm")}</div>
                          <div className="text-gray-400">
                            → {format(new Date(auction.endTime), "dd MMM, HH:mm")}
                          </div>
                        </TableCell>
                        <TableCell className="text-right">{auction.bidCount}</TableCell>
                        <TableCell className="text-right font-medium">
                          {auction.highestBid ? formatCurrency(auction.highestBid) : "—"}
                        </TableCell>
                        <TableCell className="text-right">
                          <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                              <Button variant="ghost" size="icon" aria-label="Auction actions">
                                <MoreHorizontal className="h-4 w-4" />
                              </Button>
                            </DropdownMenuTrigger>
                            <DropdownMenuContent align="end">
                              <DropdownMenuItem asChild>
                                <Link href={`/admin/auctions/${auction.id}`}>
                                  <Eye className="mr-2 h-4 w-4" />
                                  View details
                                </Link>
                              </DropdownMenuItem>
                              {isUpcoming(auction.status) && (
                                <DropdownMenuItem
                                  onClick={() => setPending({ type: "start", auction })}
                                >
                                  <Play className="mr-2 h-4 w-4" />
                                  Start now
                                </DropdownMenuItem>
                              )}
                              {auction.status === "ACTIVE" && (
                                <DropdownMenuItem
                                  onClick={() => setPending({ type: "end", auction })}
                                >
                                  <Square className="mr-2 h-4 w-4" />
                                  End &amp; pick winner
                                </DropdownMenuItem>
                              )}
                              {(isUpcoming(auction.status) || auction.status === "ACTIVE") && (
                                <DropdownMenuItem
                                  onClick={() => setPending({ type: "cancel", auction })}
                                  className="text-red-600"
                                >
                                  <XCircle className="mr-2 h-4 w-4" />
                                  Cancel auction
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
            </div>
          )}

          <div className="flex items-center justify-between border-t border-gray-100 pt-4">
            <p className="text-sm text-gray-600">
              Page {page + 1} of {totalPages} · {(data?.totalElements ?? 0).toLocaleString()} total
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

      <CreateAuctionDialog
        open={showCreate}
        onOpenChange={setShowCreate}
        onCreated={() => {
          setShowCreate(false);
          invalidate();
        }}
      />

      <ConfirmDialog
        open={pending !== null}
        onOpenChange={(open) => !open && setPending(null)}
        title={pending ? CONFIRM_COPY[pending.type].title : ""}
        description={
          pending
            ? `${CONFIRM_COPY[pending.type].description} "${pending.auction.ideaTitle}".`
            : ""
        }
        detail={pending ? CONFIRM_COPY[pending.type].detail(pending.auction) : undefined}
        confirmLabel={pending ? CONFIRM_COPY[pending.type].confirmLabel : "Confirm"}
        destructive={pending?.type === "cancel"}
        loading={actionMutation.isPending}
        onConfirm={() => pending && actionMutation.mutate(pending)}
      />
    </div>
  );
}

/** SCHEDULED is what the backend persists; UPCOMING may still arrive from older data. */
function isUpcoming(status: string) {
  return status === "SCHEDULED" || status === "UPCOMING";
}

const SUCCESS_COPY = {
  start: "Auction started — bidding is now open",
  end: "Auction closed and the winner has been notified",
  cancel: "Auction cancelled and participants notified",
} as const;

const CONFIRM_COPY = {
  start: {
    title: "Start this auction now?",
    description: "Bidding will open immediately for",
    confirmLabel: "Start auction",
    detail: () =>
      "The scheduled start time is brought forward to now, and the creator is notified.",
  },
  end: {
    title: "End this auction?",
    description: "Bidding will close immediately for",
    confirmLabel: "End auction",
    detail: (auction: AdminAuction) =>
      auction.bidCount > 0
        ? `The highest of ${auction.bidCount} bid(s) wins. The winner and creator are notified, and this cannot be undone.`
        : "This auction has no bids, so it will close without a winner.",
  },
  cancel: {
    title: "Cancel this auction?",
    description: "This permanently cancels",
    confirmLabel: "Cancel auction",
    detail: (auction: AdminAuction) =>
      auction.bidCount > 0
        ? `${auction.bidCount} bid(s) have been placed. All of them become void and every bidder is notified.`
        : "No bids have been placed yet.",
  },
} as const;

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

/**
 * Auction creation.
 *
 * <p>The idea is chosen from a list of eligible ones rather than typed as a raw
 * numeric ID, and the starting price is kept separate from the bid increment —
 * conflating them meant a "minimum bid" of 50,000 silently required a 50,000 gap
 * between consecutive bids.</p>
 */
function CreateAuctionDialog({
  open,
  onOpenChange,
  onCreated,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onCreated: () => void;
}) {
  const [ideaId, setIdeaId] = useState("");
  const [startTime, setStartTime] = useState(defaultStart());
  const [endTime, setEndTime] = useState(defaultEnd());
  const [minBid, setMinBid] = useState("");
  const [minBidIncrement, setMinBidIncrement] = useState("");
  const [reservePrice, setReservePrice] = useState("");
  const [description, setDescription] = useState("");
  const [formError, setFormError] = useState<string | null>(null);

  // Only ideas that are live-listed and not already tied to a running auction.
  const { data: ideaPage, isLoading: loadingIdeas } = useQuery({
    queryKey: ["admin", "auctionable-ideas"],
    queryFn: () => adminService.getIdeas({ status: "PUBLISHED", size: 100 }),
    enabled: open,
  });

  const { data: settings } = useQuery({
    queryKey: ["admin", "settings"],
    queryFn: () => adminService.getSettings(),
    enabled: open,
  });

  const eligibleIdeas = (ideaPage?.content ?? []).filter((idea) => !idea.hasActiveAuction);

  const createMutation = useMutation({
    mutationFn: (payload: CreateAuctionPayload) => adminService.createAuction(payload),
    onSuccess: () => {
      toast.success("Auction scheduled");
      reset();
      onCreated();
    },
    onError: (err) => setFormError(getErrorMessage(err)),
  });

  const reset = () => {
    setIdeaId("");
    setStartTime(defaultStart());
    setEndTime(defaultEnd());
    setMinBid("");
    setMinBidIncrement("");
    setReservePrice("");
    setDescription("");
    setFormError(null);
  };

  const handleSubmit = (event: React.FormEvent) => {
    event.preventDefault();
    setFormError(null);

    if (!ideaId) {
      setFormError("Select the idea this auction is for.");
      return;
    }
    if (new Date(endTime) <= new Date(startTime)) {
      setFormError("The end time must be after the start time.");
      return;
    }

    createMutation.mutate({
      ideaId: Number(ideaId),
      startTime,
      endTime,
      minBid: minBid ? Number(minBid) : undefined,
      minBidIncrement: minBidIncrement ? Number(minBidIncrement) : undefined,
      reservePrice: reservePrice ? Number(reservePrice) : undefined,
      description: description || undefined,
    });
  };

  return (
    <Dialog
      open={open}
      onOpenChange={(next) => {
        if (!next) reset();
        onOpenChange(next);
      }}
    >
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle>Create auction</DialogTitle>
          <DialogDescription>
            Schedule an auction for a published idea. It opens automatically at the start
            time unless auto-start is disabled in Settings.
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <Label htmlFor="idea">Idea</Label>
            <select
              id="idea"
              value={ideaId}
              onChange={(e) => setIdeaId(e.target.value)}
              required
              className="mt-1 w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm text-slate-900 focus:border-violet-500 focus:outline-none focus:ring-1 focus:ring-violet-500"
            >
              <option value="">
                {loadingIdeas ? "Loading ideas…" : "Select a published idea"}
              </option>
              {eligibleIdeas.map((idea) => (
                <option key={idea.id} value={idea.id}>
                  #{idea.id} · {idea.title} ({formatCurrency(idea.basePrice)})
                </option>
              ))}
            </select>
            {!loadingIdeas && eligibleIdeas.length === 0 && (
              <p className="mt-1 text-xs text-amber-700">
                No eligible ideas. An idea must be published and not already have a live or
                scheduled auction.
              </p>
            )}
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <Label htmlFor="start-time">Start time</Label>
              <Input
                id="start-time"
                type="datetime-local"
                value={startTime}
                onChange={(e) => setStartTime(e.target.value)}
                required
              />
            </div>
            <div>
              <Label htmlFor="end-time">End time</Label>
              <Input
                id="end-time"
                type="datetime-local"
                value={endTime}
                onChange={(e) => setEndTime(e.target.value)}
                required
              />
            </div>
          </div>

          <div className="grid gap-4 sm:grid-cols-3">
            <div>
              <Label htmlFor="min-bid">Starting price</Label>
              <Input
                id="min-bid"
                type="number"
                min={0.01}
                step="0.01"
                value={minBid}
                onChange={(e) => setMinBid(e.target.value)}
                placeholder="Idea base price"
              />
              <p className="mt-1 text-xs text-gray-500">Lowest acceptable first bid.</p>
            </div>
            <div>
              <Label htmlFor="min-increment">Bid increment</Label>
              <Input
                id="min-increment"
                type="number"
                min={0.01}
                step="0.01"
                value={minBidIncrement}
                onChange={(e) => setMinBidIncrement(e.target.value)}
                placeholder={settings ? String(settings.defaultBidIncrement) : "Platform default"}
              />
              <p className="mt-1 text-xs text-gray-500">Minimum step between bids.</p>
            </div>
            <div>
              <Label htmlFor="reserve">Reserve price</Label>
              <Input
                id="reserve"
                type="number"
                min={0.01}
                step="0.01"
                value={reservePrice}
                onChange={(e) => setReservePrice(e.target.value)}
                placeholder="Optional"
              />
              <p className="mt-1 text-xs text-gray-500">Internal target, not shown to bidders.</p>
            </div>
          </div>

          <div>
            <Label htmlFor="auction-description">Notes</Label>
            <Textarea
              id="auction-description"
              rows={3}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Optional context recorded against this auction"
            />
          </div>

          {formError && (
            <p role="alert" className="rounded-md bg-red-50 p-3 text-sm text-red-700">
              {formError}
            </p>
          )}

          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              Cancel
            </Button>
            <Button type="submit" disabled={createMutation.isPending}>
              {createMutation.isPending ? "Creating…" : "Create auction"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

/** `datetime-local` needs a local-time string, so the UTC offset is applied first. */
function toLocalInput(date: Date): string {
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60_000);
  return local.toISOString().slice(0, 16);
}

function defaultStart(): string {
  return toLocalInput(new Date(Date.now() + 15 * 60_000));
}

function defaultEnd(): string {
  return toLocalInput(new Date(Date.now() + 24 * 60 * 60_000));
}
