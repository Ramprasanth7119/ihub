"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuthStore } from "@/store/auth-store";
import { CreatorDashboard } from "@/features/dashboard/creator-dashboard";
import { InvestorDashboard } from "@/features/dashboard/investor-dashboard";
import { Skeleton } from "@/components/ui/skeleton";
import { getDefaultDashboardPath } from "@/lib/middleware-auth";

export default function DashboardPage() {
  const router = useRouter();
  const role = useAuthStore((s) => s.role);
  const isHydrated = useAuthStore((s) => s.isHydrated);

  useEffect(() => {
    if (isHydrated && role === "ADMIN") {
      router.replace("/admin/dashboard");
    }
  }, [isHydrated, role, router]);

  if (!isHydrated || role === "ADMIN") {
    return (
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {Array.from({ length: 4 }).map((_, i) => (
          <Skeleton key={i} className="h-32" />
        ))}
      </div>
    );
  }

  if (role === "CREATOR") return <CreatorDashboard />;
  return <InvestorDashboard />;
}
