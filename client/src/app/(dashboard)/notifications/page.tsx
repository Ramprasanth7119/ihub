"use client";

import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Bell, CheckCheck } from "lucide-react";
import { formatDistanceToNow } from "date-fns";
import { PageHeader } from "@/components/shared/page-header";
import { EmptyState } from "@/components/shared/empty-state";
import { Skeleton } from "@/components/ui/skeleton";
import { Button } from "@/components/ui/button";
import { notificationService } from "@/services/notification.service";
import { useNotificationStore } from "@/store/notification-store";
import { cn } from "@/lib/utils";

const typeIcons: Record<string, string> = {
  OUTBID: "🔔",
  AUCTION_STARTED: "🚀",
  AUCTION_ENDED: "⏰",
  WINNER_ANNOUNCED: "🏆",
  NEW_BID: "💰",
};

export default function NotificationsPage() {
  const [page, setPage] = useState(0);
  const queryClient = useQueryClient();
  const { liveNotifications, markReadLocally, setUnreadCount } = useNotificationStore();

  const { data, isLoading } = useQuery({
    queryKey: ["notifications", page],
    queryFn: () => notificationService.getAll(false, page, 20),
  });

  const markReadMutation = useMutation({
    mutationFn: (id: number) => notificationService.markAsRead(id),
    onSuccess: (_, id) => {
      markReadLocally(id);
      queryClient.invalidateQueries({ queryKey: ["notifications"] });
    },
  });

  const markAllMutation = useMutation({
    mutationFn: () => notificationService.markAllAsRead(),
    onSuccess: () => {
      setUnreadCount(0);
      queryClient.invalidateQueries({ queryKey: ["notifications"] });
    },
  });

  const allNotifications = [
    ...liveNotifications.filter(
      (ln) => !data?.content.some((n) => n.id === ln.id)
    ),
    ...(data?.content ?? []),
  ];

  return (
    <div className="mx-auto max-w-2xl">
      <PageHeader
        title="Notifications"
        description="Stay updated on bids, auctions, and wins."
        action={
          <Button
            variant="secondary"
            size="sm"
            onClick={() => markAllMutation.mutate()}
            disabled={markAllMutation.isPending}
          >
            <CheckCheck className="h-4 w-4" />
            Mark all read
          </Button>
        }
      />

      {isLoading ? (
        <div className="space-y-3">
          {Array.from({ length: 5 }).map((_, i) => (
            <Skeleton key={i} className="h-20" />
          ))}
        </div>
      ) : allNotifications.length === 0 ? (
        <EmptyState
          icon={Bell}
          title="No notifications"
          description="You'll be notified about bids, auction events, and wins."
        />
      ) : (
        <div className="space-y-2">
          {allNotifications.map((n) => (
            <button
              key={n.id}
              onClick={() => !n.read && markReadMutation.mutate(n.id)}
              className={cn(
                "w-full rounded-xl border p-4 text-left transition-colors",
                n.read
                  ? "border-white/5 bg-white/[0.02]"
                  : "border-violet-500/20 bg-violet-500/5 hover:bg-violet-500/10"
              )}
            >
              <div className="flex items-start gap-3">
                <span className="text-xl">{typeIcons[n.type] ?? "📌"}</span>
                <div className="min-w-0 flex-1">
                  <p className="font-medium text-white">{n.title}</p>
                  <p className="mt-0.5 text-sm text-slate-400">{n.message}</p>
                  <p className="mt-1 text-xs text-slate-600">
                    {formatDistanceToNow(new Date(n.createdAt), { addSuffix: true })}
                  </p>
                </div>
                {!n.read && (
                  <span className="h-2 w-2 shrink-0 rounded-full bg-violet-500" />
                )}
              </div>
            </button>
          ))}
        </div>
      )}

      {data && data.totalElements > 20 && (
        <div className="mt-6 flex justify-center gap-2">
          <Button variant="secondary" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
            Previous
          </Button>
          <Button
            variant="secondary"
            disabled={(page + 1) * 20 >= data.totalElements}
            onClick={() => setPage((p) => p + 1)}
          >
            Next
          </Button>
        </div>
      )}
    </div>
  );
}
