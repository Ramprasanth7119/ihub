import type { UserRole } from "@/types";

interface JwtPayload {
  sub: string;
  role: UserRole;
  exp: number;
}

export function decodeTokenPayload(token: string): JwtPayload | null {
  try {
    const base64 = token.split(".")[1];
    if (!base64) return null;
    const json = atob(base64.replace(/-/g, "+").replace(/_/g, "/"));
    return JSON.parse(json) as JwtPayload;
  } catch {
    return null;
  }
}

export function getRoleFromToken(token: string): UserRole | null {
  return decodeTokenPayload(token)?.role ?? null;
}

export function getDefaultDashboardPath(role: UserRole | null): string {
  if (role === "ADMIN") return "/admin/dashboard";
  return "/dashboard";
}
