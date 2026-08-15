import { type ClassValue, clsx } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export function formatCurrency(amount: number): string {
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
    maximumFractionDigits: 0,
  }).format(amount);
}

export function formatDate(date: string): string {
  return new Intl.DateTimeFormat("en-US", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(date));
}

export function getStatusColor(status: string): string {
  const map: Record<string, string> = {
    ACTIVE: "bg-emerald-500/15 text-emerald-400 border-emerald-500/30",
    // The backend persists SCHEDULED; UPCOMING is only ever an inbound alias, so
    // both are mapped to keep either spelling from falling through to grey.
    SCHEDULED: "bg-amber-500/15 text-amber-400 border-amber-500/30",
    UPCOMING: "bg-amber-500/15 text-amber-400 border-amber-500/30",
    CLOSED: "bg-slate-500/15 text-slate-400 border-slate-500/30",
    CANCELLED: "bg-red-500/15 text-red-400 border-red-500/30",
    PUBLISHED: "bg-violet-500/15 text-violet-400 border-violet-500/30",
    DRAFT: "bg-slate-500/15 text-slate-400 border-slate-500/30",
    PENDING: "bg-amber-500/15 text-amber-400 border-amber-500/30",
    APPROVED: "bg-emerald-500/15 text-emerald-400 border-emerald-500/30",
    REJECTED: "bg-red-500/15 text-red-400 border-red-500/30",
    SUSPENDED: "bg-orange-500/15 text-orange-400 border-orange-500/30",
    ARCHIVED: "bg-slate-500/15 text-slate-500 border-slate-500/30",
    NONE: "bg-slate-500/15 text-slate-400 border-slate-500/30",
  };
  return map[status?.toUpperCase()] ?? "bg-slate-500/15 text-slate-400 border-slate-500/30";
}

/** Human-facing label for an auction status. */
export function formatAuctionStatus(status: string): string {
  const normalised = status?.toUpperCase();
  if (normalised === "SCHEDULED" || normalised === "UPCOMING") return "Upcoming";
  if (normalised === "ACTIVE") return "Live";
  if (normalised === "CLOSED") return "Completed";
  if (normalised === "CANCELLED") return "Cancelled";
  return status ?? "Unknown";
}

/**
 * Light-theme status classes for the admin console, which uses a white surface
 * rather than the dark glassmorphism of the public app.
 */
export function getAdminStatusColor(status: string): string {
  const map: Record<string, string> = {
    ACTIVE: "bg-emerald-50 text-emerald-700 border-emerald-200",
    SCHEDULED: "bg-amber-50 text-amber-700 border-amber-200",
    UPCOMING: "bg-amber-50 text-amber-700 border-amber-200",
    CLOSED: "bg-slate-100 text-slate-700 border-slate-200",
    CANCELLED: "bg-red-50 text-red-700 border-red-200",
    PUBLISHED: "bg-violet-50 text-violet-700 border-violet-200",
    APPROVED: "bg-emerald-50 text-emerald-700 border-emerald-200",
    PENDING: "bg-amber-50 text-amber-700 border-amber-200",
    REJECTED: "bg-red-50 text-red-700 border-red-200",
    SUSPENDED: "bg-orange-50 text-orange-700 border-orange-200",
    DRAFT: "bg-slate-100 text-slate-700 border-slate-200",
    ARCHIVED: "bg-slate-100 text-slate-500 border-slate-200",
  };
  return map[status?.toUpperCase()] ?? "bg-slate-100 text-slate-700 border-slate-200";
}
