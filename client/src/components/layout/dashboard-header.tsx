"use client";

import Link from "next/link";
import { Bell, Menu, Zap } from "lucide-react";
import { useNotificationStore } from "@/store/notification-store";
import { useAuthStore } from "@/store/auth-store";
import { Button } from "@/components/ui/button";

interface DashboardHeaderProps {
  onMenuClick?: () => void;
}

export function DashboardHeader({ onMenuClick }: DashboardHeaderProps) {
  const unreadCount = useNotificationStore((s) => s.unreadCount);
  const user = useAuthStore((s) => s.user);

  return (
    <header className="flex h-16 items-center justify-between border-b border-white/5 bg-slate-950/50 px-4 backdrop-blur-xl lg:px-8">
      <div className="flex items-center gap-3 lg:hidden">
        <button onClick={onMenuClick} className="text-white" aria-label="Open menu">
          <Menu className="h-6 w-6" />
        </button>
        <div className="flex items-center gap-2">
          <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-gradient-to-br from-violet-500 to-indigo-600">
            <Zap className="h-3.5 w-3.5 text-white" />
          </div>
          <span className="font-bold text-white">IHub</span>
        </div>
      </div>

      <div className="hidden lg:block">
        <p className="text-sm text-slate-400">
          Welcome back, <span className="text-white">{user?.name?.split(" ")[0] ?? "there"}</span>
        </p>
      </div>

      <div className="flex items-center gap-2">
        <Button variant="ghost" size="icon" asChild className="relative">
          <Link href="/notifications" aria-label="Notifications">
            <Bell className="h-5 w-5" />
            {unreadCount > 0 && (
              <span className="absolute -right-0.5 -top-0.5 flex h-4 w-4 items-center justify-center rounded-full bg-violet-500 text-[10px] text-white">
                {unreadCount > 9 ? "9+" : unreadCount}
              </span>
            )}
          </Link>
        </Button>
      </div>
    </header>
  );
}
