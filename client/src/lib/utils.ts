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
  };
  return map[status] ?? "bg-slate-500/15 text-slate-400 border-slate-500/30";
}
