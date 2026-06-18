export const API_BASE = process.env.NEXT_PUBLIC_API_URL ?? "";
export const WS_URL = process.env.NEXT_PUBLIC_WS_URL ?? "http://localhost:8081/ws";

export const AUCTION_STATUSES = ["UPCOMING", "ACTIVE", "CLOSED"] as const;
export const IDEA_STATUSES = [
  "DRAFT",
  "PENDING",
  "APPROVED",
  "REJECTED",
  "SUSPENDED",
  "PUBLISHED",
  "ARCHIVED",
] as const;

export const IDEA_STATUS_LABELS: Record<string, string> = {
  DRAFT: "Draft — edit and publish when ready",
  PENDING: "Pending — awaiting admin review",
  APPROVED: "Approved — ready for auction",
  REJECTED: "Rejected — contact support to resubmit",
  SUSPENDED: "Suspended — temporarily unavailable",
  PUBLISHED: "Published — visible to investors",
  ARCHIVED: "Archived",
};

export const SORT_OPTIONS = [
  { value: "relevance", label: "Relevance" },
  { value: "budget_asc", label: "Budget: Low to High" },
  { value: "budget_desc", label: "Budget: High to Low" },
  { value: "newest", label: "Newest" },
] as const;

export const NAV_CREATOR = [
  { href: "/dashboard", label: "Dashboard", icon: "LayoutDashboard" },
  { href: "/ideas", label: "My Ideas", icon: "Lightbulb" },
  { href: "/ideas/new", label: "Submit Idea", icon: "Plus" },
  { href: "/auctions", label: "Auctions", icon: "Gavel" },
  { href: "/notifications", label: "Notifications", icon: "Bell" },
  { href: "/profile", label: "Profile", icon: "User" },
] as const;

export const NAV_INVESTOR = [
  { href: "/dashboard", label: "Dashboard", icon: "LayoutDashboard" },
  { href: "/search", label: "Discover", icon: "Search" },
  { href: "/ideas", label: "Ideas", icon: "Lightbulb" },
  { href: "/auctions", label: "Auctions", icon: "Gavel" },
  { href: "/notifications", label: "Notifications", icon: "Bell" },
  { href: "/profile", label: "Profile", icon: "User" },
] as const;
