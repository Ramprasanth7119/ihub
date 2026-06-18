import { create } from "zustand";
import { persist } from "zustand/middleware";
import type { User, UserRole } from "@/types";
import { decodeJwt } from "@/lib/jwt";

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  role: UserRole | null;
  email: string | null;
  user: User | null;
  isHydrated: boolean;
  setTokens: (access: string, refresh: string, role: string) => void;
  setUser: (user: User) => void;
  logout: () => void;
  setHydrated: () => void;
  isAuthenticated: () => boolean;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      accessToken: null,
      refreshToken: null,
      role: null,
      email: null,
      user: null,
      isHydrated: false,

      setTokens: (access, refresh, role) => {
        const payload = decodeJwt(access);
        if (typeof document !== "undefined") {
          document.cookie = `ihub-auth-token=${access}; path=/; max-age=${60 * 60 * 24 * 7}; SameSite=Lax`;
        }
        set({
          accessToken: access,
          refreshToken: refresh,
          role: role as UserRole,
          email: payload?.sub ?? null,
        });
      },

      setUser: (user) => set({ user }),

      logout: () => {
        if (typeof document !== "undefined") {
          document.cookie = "ihub-auth-token=; path=/; max-age=0";
        }
        set({
          accessToken: null,
          refreshToken: null,
          role: null,
          email: null,
          user: null,
        });
      },

      setHydrated: () => set({ isHydrated: true }),

      isAuthenticated: () => !!get().accessToken,
    }),
    {
      name: "ihub-auth",
      partialize: (s) => ({
        accessToken: s.accessToken,
        refreshToken: s.refreshToken,
        role: s.role,
        email: s.email,
        user: s.user,
      }),
      onRehydrateStorage: () => (state) => {
        if (state?.accessToken && typeof document !== "undefined") {
          document.cookie = `ihub-auth-token=${state.accessToken}; path=/; max-age=${60 * 60 * 24 * 7}; SameSite=Lax`;
        }
        state?.setHydrated();
      },
    }
  )
);
