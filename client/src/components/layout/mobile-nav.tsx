"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { Bell, Gavel, LayoutDashboard, Lightbulb, Search, User } from "lucide-react";
import { cn } from "@/lib/utils";
import { useAuthStore } from "@/store/auth-store";

export function MobileNav() {
  const pathname = usePathname();
  const role = useAuthStore((s) => s.role);

  const items =
    role === "CREATOR"
      ? [
          { href: "/dashboard", icon: LayoutDashboard, label: "Home" },
          { href: "/ideas", icon: Lightbulb, label: "Ideas" },
          { href: "/auctions", icon: Gavel, label: "Auctions" },
          { href: "/notifications", icon: Bell, label: "Alerts" },
          { href: "/profile", icon: User, label: "Profile" },
        ]
      : [
          { href: "/dashboard", icon: LayoutDashboard, label: "Home" },
          { href: "/search", icon: Search, label: "Discover" },
          { href: "/auctions", icon: Gavel, label: "Auctions" },
          { href: "/notifications", icon: Bell, label: "Alerts" },
          { href: "/profile", icon: User, label: "Profile" },
        ];

  return (
    <nav className="fixed inset-x-0 bottom-0 z-50 border-t border-white/10 bg-slate-950/90 backdrop-blur-xl lg:hidden">
      <div className="flex items-center justify-around py-2">
        {items.map((item) => {
          const active = pathname === item.href || pathname.startsWith(item.href + "/");
          return (
            <Link
              key={item.href}
              href={item.href}
              className={cn(
                "flex flex-col items-center gap-0.5 px-3 py-1 text-[10px]",
                active ? "text-violet-400" : "text-slate-500"
              )}
            >
              <item.icon className="h-5 w-5" />
              {item.label}
            </Link>
          );
        })}
      </div>
    </nav>
  );
}
