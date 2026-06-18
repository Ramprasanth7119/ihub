"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { MoreHorizontal } from "lucide-react";
import { cn } from "@/lib/utils";
import { ADMIN_MOBILE_NAV_ITEMS, ADMIN_NAV_ITEMS } from "@/constants/admin-nav";

interface AdminMobileNavProps {
  drawerOpen: boolean;
  onDrawerToggle: () => void;
  onDrawerClose: () => void;
}

export function AdminMobileNav({ drawerOpen, onDrawerToggle, onDrawerClose }: AdminMobileNavProps) {
  const pathname = usePathname();
  const overflowItems = ADMIN_NAV_ITEMS.slice(5);

  return (
    <>
      {drawerOpen && (
        <div className="fixed inset-0 z-50 lg:hidden">
          <button
            className="absolute inset-0 bg-black/40"
            onClick={onDrawerClose}
            aria-label="Close menu"
          />
          <div className="absolute bottom-16 left-0 right-0 max-h-[60vh] overflow-y-auto rounded-t-2xl bg-white p-4 shadow-xl">
            <p className="mb-3 text-sm font-semibold text-gray-900">More</p>
            <nav className="space-y-1">
              {overflowItems.map((item) => {
                const Icon = item.icon;
                const isActive = pathname === item.href;
                return (
                  <Link
                    key={item.href}
                    href={item.href}
                    onClick={onDrawerClose}
                    className={cn(
                      "flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium",
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

      <nav className="fixed inset-x-0 bottom-0 z-40 border-t bg-white lg:hidden">
        <div className="flex items-center justify-around py-2">
          {ADMIN_MOBILE_NAV_ITEMS.map((item) => {
            const Icon = item.icon;
            const isActive = pathname === item.href;
            return (
              <Link
                key={item.href}
                href={item.href}
                className={cn(
                  "flex flex-col items-center gap-0.5 px-2 py-1 text-[10px]",
                  isActive ? "text-gray-900" : "text-gray-500"
                )}
              >
                <Icon className="h-5 w-5" />
                {item.label}
              </Link>
            );
          })}
          <button
            onClick={onDrawerToggle}
            className={cn(
              "flex flex-col items-center gap-0.5 px-2 py-1 text-[10px]",
              overflowItems.some((i) => pathname === i.href) ? "text-gray-900" : "text-gray-500"
            )}
          >
            <MoreHorizontal className="h-5 w-5" />
            More
          </button>
        </div>
      </nav>
    </>
  );
}
