import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";
import { getDefaultDashboardPath, getRoleFromToken } from "@/lib/middleware-auth";

const authPaths = ["/login", "/register"];

export function middleware(request: NextRequest) {
  const token = request.cookies.get("ihub-auth-token")?.value;
  const { pathname } = request.nextUrl;
  const role = token ? getRoleFromToken(token) : null;

  const isAdminRoute = pathname.startsWith("/admin");
  const needsAuth =
    pathname.startsWith("/dashboard") ||
    pathname.startsWith("/notifications") ||
    pathname.startsWith("/profile") ||
    pathname.startsWith("/search") ||
    pathname.startsWith("/auctions") ||
    pathname.startsWith("/ideas") ||
    isAdminRoute;

  const isAuthPage = authPaths.some((p) => pathname.startsWith(p));

  if (needsAuth && !token) {
    const login = new URL("/login", request.url);
    login.searchParams.set("redirect", pathname);
    return NextResponse.redirect(login);
  }

  if (isAuthPage && token) {
    return NextResponse.redirect(new URL(getDefaultDashboardPath(role), request.url));
  }

  if (isAdminRoute && token && role !== "ADMIN") {
    return NextResponse.redirect(new URL("/dashboard", request.url));
  }

  if (
    token &&
    role === "ADMIN" &&
    (pathname.startsWith("/dashboard") ||
      pathname.startsWith("/ideas") ||
      pathname.startsWith("/auctions") ||
      pathname.startsWith("/search") ||
      pathname.startsWith("/profile") ||
      pathname.startsWith("/notifications"))
  ) {
    return NextResponse.redirect(new URL("/admin/dashboard", request.url));
  }

  return NextResponse.next();
}

export const config = {
  matcher: [
    "/dashboard/:path*",
    "/ideas/:path*",
    "/auctions/:path*",
    "/search/:path*",
    "/notifications/:path*",
    "/profile/:path*",
    "/admin/:path*",
    "/login",
    "/register",
  ],
};
