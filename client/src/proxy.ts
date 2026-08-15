import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";
import { getDefaultDashboardPath, getRoleFromToken } from "@/lib/middleware-auth";

/**
 * Route protection at the network boundary.
 *
 * <p>Renamed from `middleware.ts`: Next.js 16 deprecates that convention in favour
 * of `proxy`, which runs on the Node.js runtime.</p>
 *
 * <p>This is a routing convenience, not the security boundary — it only reads a
 * non-HttpOnly cookie the client sets, so it can be spoofed. Real authorization is
 * enforced by the Spring Boot API on every request; this exists so visitors are
 * redirected before a protected page renders and flashes empty.</p>
 */

const AUTH_PATHS = ["/login", "/register"];

/** Routes that require any signed-in user. */
const PROTECTED_PREFIXES = [
  "/dashboard",
  "/notifications",
  "/profile",
  "/search",
  "/auctions",
  "/ideas",
  "/my-bids",
];

/** Routes an administrator is redirected away from, into the admin console. */
const NON_ADMIN_PREFIXES = [
  "/dashboard",
  "/ideas",
  "/auctions",
  "/search",
  "/profile",
  "/notifications",
  "/my-bids",
];

export function proxy(request: NextRequest) {
  const token = request.cookies.get("ihub-auth-token")?.value;
  const { pathname } = request.nextUrl;

  // An expired token is treated as no token, so a stale cookie sends the visitor
  // to sign in rather than onto a page whose every request will 401.
  const validToken = token && !isExpired(token) ? token : undefined;
  const role = validToken ? getRoleFromToken(validToken) : null;

  const isAdminRoute = pathname.startsWith("/admin");
  const needsAuth = isAdminRoute || PROTECTED_PREFIXES.some((prefix) => pathname.startsWith(prefix));
  const isAuthPage = AUTH_PATHS.some((path) => pathname.startsWith(path));

  if (needsAuth && !validToken) {
    const login = new URL("/login", request.url);
    login.searchParams.set("redirect", pathname);
    if (token) {
      // Distinguishes "your session ended" from "please sign in".
      login.searchParams.set("reason", "expired");
    }
    return NextResponse.redirect(login);
  }

  if (isAuthPage && validToken) {
    return NextResponse.redirect(new URL(getDefaultDashboardPath(role), request.url));
  }

  if (isAdminRoute && validToken && role !== "ADMIN") {
    return NextResponse.redirect(new URL("/dashboard", request.url));
  }

  if (
    validToken &&
    role === "ADMIN" &&
    NON_ADMIN_PREFIXES.some((prefix) => pathname.startsWith(prefix))
  ) {
    return NextResponse.redirect(new URL("/admin/dashboard", request.url));
  }

  return NextResponse.next();
}

/** Reads the `exp` claim without verifying the signature — the API does that. */
function isExpired(token: string): boolean {
  const payload = decodeExp(token);
  if (payload === null) return true;
  return payload * 1000 <= Date.now();
}

function decodeExp(token: string): number | null {
  try {
    const base64 = token.split(".")[1];
    if (!base64) return null;
    const json = atob(base64.replace(/-/g, "+").replace(/_/g, "/"));
    const parsed = JSON.parse(json) as { exp?: number };
    return typeof parsed.exp === "number" ? parsed.exp : null;
  } catch {
    return null;
  }
}

export const config = {
  matcher: [
    "/dashboard/:path*",
    "/ideas/:path*",
    "/auctions/:path*",
    "/search/:path*",
    "/notifications/:path*",
    "/profile/:path*",
    "/my-bids/:path*",
    "/admin/:path*",
    "/login",
    "/register",
  ],
};
