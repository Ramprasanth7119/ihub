import {
  LayoutDashboard,
  Users,
  Lightbulb,
  Gavel,
  DollarSign,
  Trophy,
  FileText,
  Settings,
  BarChart3,
  LucideIcon,
} from "lucide-react";

export interface AdminNavItem {
  href: string;
  label: string;
  icon: LucideIcon;
}

export const ADMIN_NAV_ITEMS: AdminNavItem[] = [
  { href: "/admin/dashboard", label: "Dashboard", icon: LayoutDashboard },
  { href: "/admin/users", label: "Users", icon: Users },
  { href: "/admin/ideas", label: "Ideas", icon: Lightbulb },
  { href: "/admin/auctions", label: "Auctions", icon: Gavel },
  { href: "/admin/bids", label: "Bids", icon: DollarSign },
  { href: "/admin/winners", label: "Winners", icon: Trophy },
  { href: "/admin/audit-logs", label: "Audit Logs", icon: FileText },
  { href: "/admin/reports", label: "Reports", icon: BarChart3 },
  { href: "/admin/settings", label: "Settings", icon: Settings },
];

export const ADMIN_MOBILE_NAV_ITEMS = ADMIN_NAV_ITEMS.slice(0, 5);
