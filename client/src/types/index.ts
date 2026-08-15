export type UserRole = "CREATOR" | "INVESTOR" | "ADMIN";

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  role: UserRole;
}

export interface User {
  id: number;
  name: string;
  /**
   * Only returned for your own account or to an administrator. Reading another
   * user's profile yields `null` here so the endpoint cannot be used to harvest
   * email addresses.
   */
  email: string | null;
  role: UserRole;
}

export interface Idea {
  id: number;
  creatorId: number;
  title: string;
  description: string;
  category: string;
  basePrice: number;
  maxBudget?: number;
  status: string;
  tags?: string[];
}

export interface IdeaDocument {
  id: number;
  title: string;
  description: string;
  category: string;
  minBudget?: number;
  maxBudget?: number;
  ideaStatus?: string;
  auctionStatus?: string;
  tags?: string[];
}

export interface SearchResponse {
  content: IdeaDocument[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
  sort?: string;
}

export interface Category {
  id: number;
  name: string;
  slug: string;
}

export interface CategoryFacet {
  category: string;
  count: number;
}

export interface Auction {
  id: number;
  ideaId: number;
  startTime: string;
  endTime: string;
  minBidIncrement?: number;
  status: string;
}

export interface BidResponse {
  message: string;
  bidId: number;
  auctionId: number;
  amount: number;
  currentHighest: number;
  rank: number;
}

export interface BidUpdate {
  auctionId: number;
  investorId: number;
  amount: number;
  rank: number;
  timestamp: string;
}

export interface HighestBid {
  auctionId: number;
  bidId: number;
  investorId: number;
  investorName: string;
  amount: number;
  placedAt: string;
}

export interface LeaderboardEntry {
  rank: number;
  investorId: number;
  investorName: string;
  highestBid: number;
  lastBidAt: string;
}

export interface BidHistoryEntry {
  bidId: number;
  investorId: number;
  investorName: string;
  amount: number;
  placedAt: string;
}

export interface AuctionHistoryEntry {
  id: number;
  eventType: string;
  details: string;
  createdAt: string;
}

export interface Notification {
  id: number;
  type: string;
  title: string;
  message: string;
  referenceType?: string;
  referenceId?: number;
  read: boolean;
  createdAt: string;
}

export interface NotificationPage {
  content: Notification[];
  totalElements: number;
  unreadCount: number;
  page: number;
  size: number;
}

export interface AuctionWinner {
  auctionId: number;
  ideaId: number;
  winnerId: number;
  winnerName: string;
  winningBid: number;
  closedAt: string;
}

/** One of an investor's own bids, with enough auction context to render standalone. */
export interface InvestorBid {
  bidId: number;
  auctionId: number;
  ideaId: number;
  ideaTitle: string;
  amount: number;
  highestBid: number;
  auctionStatus: string;
  endTime: string;
  placedAt: string;
  /** This bid is currently the highest on its auction. */
  leading: boolean;
  /** The auction closed and this investor won it. */
  won: boolean;
}

/** A decided auction joined with its idea, creator and winner. */
export interface AuctionWinnerSummary {
  auctionId: number;
  ideaId: number;
  ideaTitle: string;
  ideaCategory: string;
  creatorId: number;
  creatorName: string;
  winnerId: number;
  winnerName: string;
  winnerEmail: string;
  winningBid: number;
  bidCount: number;
  endTime: string;
  decidedAt: string;
}

/** Platform settings, persisted server-side and editable by an administrator. */
export interface PlatformSettings {
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

export interface SearchHealth {
  available: boolean;
}

export interface ReindexResult {
  indexed: number;
  searchAvailable: boolean;
}

/** The error envelope every backend endpoint returns on failure. */
export interface ApiError {
  timestamp?: string;
  status: number;
  error: string;
  message: string;
  path: string;
  fieldErrors?: { field: string; message: string }[] | null;
}

// Admin Types
export interface AdminMetrics {
  totalUsers: number;
  totalCreators: number;
  totalInvestors: number;
  totalAdmins: number;
  activeUsers: number;
  totalIdeas: number;
  publishedIdeas: number;
  draftIdeas: number;
  totalAuctions: number;
  scheduledAuctions: number;
  activeAuctions: number;
  closedAuctions: number;
  totalBids: number;
  completedAuctionsWithWinner: number;
  cancelledAuctions: number;
  /** Sum of every bid ever placed — bidding activity, not revenue. */
  totalBidValue: number;
  /** Sum of winning bids across decided auctions — value actually settled. */
  settledValue: number;
  highestWinningBid: number;
  newUsersLast30Days: number;
  newUsersPrevious30Days: number;
}

export interface AdminUser {
  id: number;
  name: string;
  email: string;
  role: UserRole;
  verified: boolean;
  active: boolean;
  createdAt: string;
  ideaCount: number;
  bidCount: number;
}

export interface AdminIdea {
  id: number;
  creatorId: number;
  creatorName: string;
  creatorEmail: string;
  title: string;
  description: string;
  category: string;
  basePrice: number;
  maxBudget?: number;
  status: string;
  createdAt: string;
  auctionCount: number;
  hasActiveAuction: boolean;
}

export interface AdminAuction {
  id: number;
  ideaId: number;
  ideaTitle: string;
  status: string;
  startTime: string;
  endTime: string;
  bidCount: number;
  highestBid?: number;
}

/**
 * Admin auction creation payload.
 *
 * `minBid` is the starting price recorded against the idea; `minBidIncrement` is
 * how much each new bid must exceed the current highest by. They are separate
 * fields — the two used to be conflated, so a 50,000 minimum bid became a 50,000
 * gap required between consecutive bids.
 */
export interface CreateAuctionPayload {
  ideaId: number;
  startTime: string;
  endTime: string;
  minBid?: number;
  minBidIncrement?: number;
  reservePrice?: number;
  description?: string;
}

export interface UpdateAuctionPayload {
  startTime?: string;
  endTime?: string;
  minBidIncrement?: number;
  description?: string;
}

export interface AdminBid {
  id: number;
  auctionId: number;
  ideaTitle: string;
  investorId: number;
  investorName: string;
  investorEmail: string;
  bidAmount: number;
  createdAt: string;
  auctionStatus: string;
  rank: number;
}

export interface AdminAuditLog {
  id: number;
  adminId: number;
  adminName: string;
  adminEmail: string;
  action: string;
  entityType: string;
  entityId?: number;
  details?: string;
  ipAddress?: string;
  createdAt: string;
}

export interface AdminPageResponse<T> {
  content: T[];
  totalElements: number;
  page: number;
  size: number;
}

export interface DashboardChartData {
  ideasByCategory: { category: string; count: number }[];
  auctionStatusDistribution: { status: string; count: number }[];
  monthlyAuctions: { month: string; count: number }[];
  topInvestors: { investorId: number; investorName: string; bidCount: number; totalInvestment: number }[];
}
