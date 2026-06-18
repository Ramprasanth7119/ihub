"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  Bell,
  Gavel,
  LayoutDashboard,
  Lightbulb,
  LogOut,
  Plus,
  Search,
  User,
  Zap,
} from "lucide-react";
import { cn } from "@/lib/utils";
import { useAuthStore } from "@/store/auth-store";
import { useNotificationStore } from "@/store/notification-store";
import { authService } from "@/services/auth.service";
import { useRouter } from "next/navigation";
import { NAV_CREATOR, NAV_INVESTOR } from "@/constants";

const iconMap = {
  LayoutDashboard,
  Lightbulb,
  Plus,
  Gavel,
  Bell,
  User,
  Search,
};

export function Sidebar() {
  const pathname = usePathname();
  const router = useRouter();
  const { role, logout, refreshToken, user } = useAuthStore();
  const unreadCount = useNotificationStore((s) => s.unreadCount);

  const nav = role === "CREATOR" ? NAV_CREATOR : NAV_INVESTOR;

  const handleLogout = async () => {
    if (refreshToken) {
      try {
        await authService.logout(refreshToken);
      } catch {
        /* ignore */
      }
    }
    logout();
    router.push("/login");
  };

  return (
    <aside className="hidden w-64 shrink-0 flex-col border-r border-white/5 bg-slate-950/50 backdrop-blur-xl lg:flex">
      <div className="flex h-16 items-center gap-2 border-b border-white/5 px-6">
        <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-gradient-to-br from-violet-500 to-indigo-600">
          <Zap className="h-4 w-4 text-white" />
        </div>
        <span className="font-bold text-white">IHub</span>
      </div>

      <nav className="flex-1 space-y-1 p-4">
        {nav.map((item) => {
          const Icon = iconMap[item.icon as keyof typeof iconMap];
          const active = pathname === item.href || pathname.startsWith(item.href + "/");
          return (
            <Link
              key={item.href}
              href={item.href}
              className={cn(
                "flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-colors",
                active
                  ? "bg-violet-500/15 text-violet-300"
                  : "text-slate-400 hover:bg-white/5 hover:text-white"
              )}
            >
              <Icon className="h-4 w-4" />
              {item.label}
              {item.href === "/notifications" && unreadCount > 0 && (
                <span className="ml-auto flex h-5 min-w-5 items-center justify-center rounded-full bg-violet-500 px-1.5 text-xs text-white">
                  {unreadCount}
                </span>
              )}
            </Link>
          );
        })}
      </nav>

      <div className="border-t border-white/5 p-4">
        <div className="mb-3 rounded-xl bg-white/5 px-3 py-2">
          <p className="truncate text-sm font-medium text-white">{user?.name ?? "User"}</p>
          <p className="truncate text-xs text-slate-500">{role}</p>
        </div>
        <button
          onClick={handleLogout}
          className="flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-sm text-slate-400 transition-colors hover:bg-red-500/10 hover:text-red-400"
        >
          <LogOut className="h-4 w-4" />
          Sign out
        </button>
      </div>
    </aside>
  );
}
