"use client";

import { useMemo } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Bell, CheckCheck } from "lucide-react";
import { formatDistanceToNow } from "date-fns";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { notificationService } from "@/services/notification.service";
import { useNotificationStore } from "@/store/notification-store";
import { cn } from "@/lib/utils";

/**
 * Notification bell with an inline feed.
 *
 * <p>Used in the admin header, which previously rendered a bell icon with no click
 * handler and no unread badge. Admins are redirected away from the creator/investor
 * `/notifications` page, so the feed is shown in this popover instead of linking
 * to a route they cannot reach.</p>
 */
export function NotificationBell({ surface = "light" }: { surface?: "light" | "dark" }) {
  const queryClient = useQueryClient();

  // Pushed over the WebSocket; falls back to the polled count below when the
  // socket has not delivered anything yet this session.
  const liveUnread = useNotificationStore((s) => s.unreadCount);

  const { data, isLoading } = useQuery({
    queryKey: ["notifications", "recent"],
    queryFn: () => notificationService.getAll(false, 0, 8),
    refetchInterval: 60_000,
  });

  const unread = Math.max(liveUnread, data?.unreadCount ?? 0);
  const notifications = useMemo(() => data?.content ?? [], [data]);

  const markAllMutation = useMutation({
    mutationFn: () => notificationService.markAllAsRead(),
    onSuccess: () => {
      useNotificationStore.getState().setUnreadCount(0);
      queryClient.invalidateQueries({ queryKey: ["notifications"] });
    },
  });

  const markOneMutation = useMutation({
    mutationFn: (id: number) => notificationService.markAsRead(id),
    onSuccess: (_result, id) => {
      useNotificationStore.getState().markReadLocally(id);
      queryClient.invalidateQueries({ queryKey: ["notifications"] });
    },
  });

  const dark = surface === "dark";

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button
          variant="ghost"
          size="icon"
          className="relative"
          aria-label={unread > 0 ? `Notifications, ${unread} unread` : "Notifications"}
        >
          <Bell className="h-5 w-5" />
          {unread > 0 && (
            <span
              className="absolute -right-0.5 -top-0.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-red-500 px-1 text-[10px] font-bold text-white"
              aria-hidden="true"
            >
              {unread > 99 ? "99+" : unread}
            </span>
          )}
        </Button>
      </DropdownMenuTrigger>

      <DropdownMenuContent align="end" className="w-80 p-0">
        <div
          className={cn(
            "flex items-center justify-between border-b px-4 py-3",
            dark ? "border-white/10" : "border-slate-200"
          )}
        >
          <p className={cn("text-sm font-semibold", dark ? "text-white" : "text-slate-900")}>
            Notifications
          </p>
          {unread > 0 && (
            <button
              type="button"
              onClick={() => markAllMutation.mutate()}
              disabled={markAllMutation.isPending}
              className="inline-flex items-center gap-1 text-xs font-medium text-violet-600 hover:underline disabled:opacity-50"
            >
              <CheckCheck className="h-3.5 w-3.5" />
              Mark all read
            </button>
          )}
        </div>

        <div className="max-h-96 overflow-y-auto">
          {isLoading ? (
            <p className="px-4 py-8 text-center text-sm text-slate-500">Loading…</p>
          ) : notifications.length === 0 ? (
            <div className="px-4 py-10 text-center">
              <Bell className="mx-auto mb-2 h-6 w-6 text-slate-300" />
              <p className="text-sm text-slate-500">You&apos;re all caught up</p>
            </div>
          ) : (
            <ul className={cn("divide-y", dark ? "divide-white/10" : "divide-slate-100")}>
              {notifications.map((notification) => (
                <li key={notification.id}>
                  <button
                    type="button"
                    onClick={() => !notification.read && markOneMutation.mutate(notification.id)}
                    className={cn(
                      "w-full px-4 py-3 text-left transition-colors",
                      dark ? "hover:bg-white/5" : "hover:bg-slate-50",
                      !notification.read && (dark ? "bg-violet-500/10" : "bg-violet-50/60")
                    )}
                  >
                    <div className="flex items-start gap-2">
                      {!notification.read && (
                        <span className="mt-1.5 h-2 w-2 shrink-0 rounded-full bg-violet-500" />
                      )}
                      <div className={cn("min-w-0", notification.read && "pl-4")}>
                        <p
                          className={cn(
                            "truncate text-sm font-medium",
                            dark ? "text-white" : "text-slate-900"
                          )}
                        >
                          {notification.title}
                        </p>
                        <p
                          className={cn(
                            "mt-0.5 line-clamp-2 text-xs",
                            dark ? "text-slate-400" : "text-slate-600"
                          )}
                        >
                          {notification.message}
                        </p>
                        <p className="mt-1 text-[11px] text-slate-400">
                          {formatDistanceToNow(new Date(notification.createdAt), {
                            addSuffix: true,
                          })}
                        </p>
                      </div>
                    </div>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
