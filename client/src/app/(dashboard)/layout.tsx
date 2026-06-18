"use client";

import { Sidebar } from "@/components/layout/sidebar";
import { DashboardHeader } from "@/components/layout/dashboard-header";
import { MobileNav } from "@/components/layout/mobile-nav";
import { useRequireAuth } from "@/hooks/use-auth";
import { useNotificationsSocket } from "@/hooks/use-notifications-socket";
import { useQuery } from "@tanstack/react-query";
import { notificationService } from "@/services/notification.service";
import { useNotificationStore } from "@/store/notification-store";
import { useEffect } from "react";
import { Skeleton } from "@/components/ui/skeleton";

export default function DashboardLayout({ children }: { children: React.ReactNode }) {
  const auth = useRequireAuth();
  const setUnreadCount = useNotificationStore((s) => s.setUnreadCount);

  useNotificationsSocket();

  const { data: unread } = useQuery({
    queryKey: ["notifications", "unread-count"],
    queryFn: () => notificationService.getUnreadCount(),
    enabled: auth.isAuthenticated(),
  });

  useEffect(() => {
    if (unread != null) setUnreadCount(unread);
  }, [unread, setUnreadCount]);

  if (!auth.isHydrated) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <Skeleton className="h-8 w-48" />
      </div>
    );
  }

  if (!auth.isAuthenticated()) return null;

  return (
    <div className="flex min-h-screen">
      <Sidebar />
      <div className="flex flex-1 flex-col pb-16 lg:pb-0">
        <DashboardHeader />
        <main className="flex-1 overflow-auto p-4 lg:p-8">{children}</main>
      </div>
      <MobileNav />
    </div>
  );
}
