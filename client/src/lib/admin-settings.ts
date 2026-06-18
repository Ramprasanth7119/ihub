const STORAGE_KEY = "ihub-admin-settings";

export interface AdminSettings {
  platformName: string;
  supportEmail: string;
  platformDescription: string;
  emailNotifications: boolean;
  ideaApprovalAlerts: boolean;
  auctionStartAlerts: boolean;
  auctionEndAlerts: boolean;
  defaultMinBid: number;
  defaultBidIncrement: number;
  auctionDurationHours: number;
  autoStartAuctions: boolean;
  autoEndAuctions: boolean;
  sessionTimeoutMinutes: number;
  sessionTimeoutEnabled: boolean;
}

export const DEFAULT_ADMIN_SETTINGS: AdminSettings = {
  platformName: "IHub",
  supportEmail: "support@ihub.com",
  platformDescription:
    "IHub is an Idea Auction Platform where creators submit ideas and investors participate in auctions.",
  emailNotifications: true,
  ideaApprovalAlerts: true,
  auctionStartAlerts: true,
  auctionEndAlerts: true,
  defaultMinBid: 100,
  defaultBidIncrement: 100,
  auctionDurationHours: 24,
  autoStartAuctions: true,
  autoEndAuctions: true,
  sessionTimeoutMinutes: 30,
  sessionTimeoutEnabled: true,
};

export function loadAdminSettings(): AdminSettings {
  if (typeof window === "undefined") return DEFAULT_ADMIN_SETTINGS;
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return DEFAULT_ADMIN_SETTINGS;
    return { ...DEFAULT_ADMIN_SETTINGS, ...JSON.parse(raw) };
  } catch {
    return DEFAULT_ADMIN_SETTINGS;
  }
}

export function saveAdminSettings(settings: AdminSettings): void {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(settings));
}
