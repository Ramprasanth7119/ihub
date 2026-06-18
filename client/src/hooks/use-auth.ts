"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuthStore } from "@/store/auth-store";
import { userService } from "@/services/user.service";
import { getDefaultDashboardPath } from "@/lib/middleware-auth";
import type { UserRole } from "@/types";

export function useAuth() {
  const store = useAuthStore();

  useEffect(() => {
    if (store.isHydrated && store.accessToken && !store.user) {
      userService.getMe().then((user) => store.setUser(user)).catch(() => {
        /* handled by axios interceptor on 401 */
      });
    }
  }, [store.isHydrated, store.accessToken, store.user, store]);

  return store;
}

export function useRequireAuth(allowedRoles?: UserRole[]) {
  const auth = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!auth.isHydrated) return;
    if (!auth.isAuthenticated()) {
      router.replace("/login");
      return;
    }
    if (allowedRoles && auth.role && !allowedRoles.includes(auth.role)) {
      router.replace(getDefaultDashboardPath(auth.role));
    }
  }, [auth.isHydrated, auth, allowedRoles, router]);

  return auth;
}
