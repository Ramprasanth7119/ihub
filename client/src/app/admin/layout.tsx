"use client";

import { useState } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { Sidebar } from "@/components/layout/admin-sidebar";
import { AdminHeader } from "@/components/layout/admin-header";
import { AdminMobileNav } from "@/components/layout/admin-mobile-nav";
import { useRequireAuth } from "@/hooks/use-auth";
import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";
import { ADMIN_NAV_ITEMS } from "@/constants/admin-nav";

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const auth = useRequireAuth();
  const pathname = usePathname();
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [sideDrawerOpen, setSideDrawerOpen] = useState(false);

  if (!auth.isHydrated) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <Skeleton className="h-8 w-48" />
      </div>
    );
  }

  if (!auth.isAuthenticated()) return null;

  if (auth.role !== "ADMIN") {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <div className="text-center">
          <h1 className="text-2xl font-bold text-gray-900">Access Denied</h1>
          <p className="mt-2 text-gray-600">You don&apos;t have permission to access this page.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="flex min-h-screen bg-gray-50">
      <Sidebar />

      {sideDrawerOpen && (
        <div className="fixed inset-0 z-50 lg:hidden">
          <button
            className="absolute inset-0 bg-black/40"
            onClick={() => setSideDrawerOpen(false)}
            aria-label="Close menu"
          />
          <div className="absolute left-0 top-0 h-full w-64 bg-white shadow-xl">
            <div className="flex h-16 items-center border-b px-6">
              <h1 className="text-xl font-bold text-gray-900">IHub Admin</h1>
            </div>
            <nav className="space-y-1 p-4">
              {ADMIN_NAV_ITEMS.map((item) => {
                const Icon = item.icon;
                const isActive = pathname === item.href;
                return (
                  <Link
                    key={item.href}
                    href={item.href}
                    onClick={() => setSideDrawerOpen(false)}
                    className={cn(
                      "flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium",
                      isActive ? "bg-gray-900 text-white" : "text-gray-700 hover:bg-gray-100"
                    )}
                  >
                    <Icon className="h-5 w-5" />
                    {item.label}
                  </Link>
                );
              })}
            </nav>
          </div>
        </div>
      )}

      <div className="flex flex-1 flex-col pb-16 lg:pb-0">
        <AdminHeader onMenuClick={() => setSideDrawerOpen(true)} />
        <main className="flex-1 overflow-auto p-4 lg:p-6">{children}</main>
      </div>

      <AdminMobileNav
        drawerOpen={drawerOpen}
        onDrawerToggle={() => setDrawerOpen((o) => !o)}
        onDrawerClose={() => setDrawerOpen(false)}
      />
    </div>
  );
}
