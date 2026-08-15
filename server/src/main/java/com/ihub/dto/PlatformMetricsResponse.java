package com.ihub.dto;

public class PlatformMetricsResponse {

    private long totalUsers;
    private long totalCreators;
    private long totalInvestors;
    private long totalAdmins;
    private long activeUsers;
    private long totalIdeas;
    private long publishedIdeas;
    private long draftIdeas;
    private long totalAuctions;
    private long scheduledAuctions;
    private long activeAuctions;
    private long closedAuctions;
    private long totalBids;
    private long completedAuctionsWithWinner;
    private long cancelledAuctions;

    /** Sum of every bid amount ever placed — total bidding activity, not revenue. */
    private double totalBidValue;

    /** Sum of the winning bids across decided auctions — the value actually settled. */
    private double settledValue;

    /** Highest single winning bid on the platform. */
    private double highestWinningBid;

    /** Users who registered in the last 30 days, for a real growth figure. */
    private long newUsersLast30Days;

    /** Users who registered in the 30 days before that, as the comparison baseline. */
    private long newUsersPrevious30Days;

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getTotalCreators() {
        return totalCreators;
    }

    public void setTotalCreators(long totalCreators) {
        this.totalCreators = totalCreators;
    }

    public long getTotalInvestors() {
        return totalInvestors;
    }

    public void setTotalInvestors(long totalInvestors) {
        this.totalInvestors = totalInvestors;
    }

    public long getTotalAdmins() {
        return totalAdmins;
    }

    public void setTotalAdmins(long totalAdmins) {
        this.totalAdmins = totalAdmins;
    }

    public long getActiveUsers() {
        return activeUsers;
    }

    public void setActiveUsers(long activeUsers) {
        this.activeUsers = activeUsers;
    }

    public long getTotalIdeas() {
        return totalIdeas;
    }

    public void setTotalIdeas(long totalIdeas) {
        this.totalIdeas = totalIdeas;
    }

    public long getPublishedIdeas() {
        return publishedIdeas;
    }

    public void setPublishedIdeas(long publishedIdeas) {
        this.publishedIdeas = publishedIdeas;
    }

    public long getDraftIdeas() {
        return draftIdeas;
    }

    public void setDraftIdeas(long draftIdeas) {
        this.draftIdeas = draftIdeas;
    }

    public long getTotalAuctions() {
        return totalAuctions;
    }

    public void setTotalAuctions(long totalAuctions) {
        this.totalAuctions = totalAuctions;
    }

    public long getScheduledAuctions() {
        return scheduledAuctions;
    }

    public void setScheduledAuctions(long scheduledAuctions) {
        this.scheduledAuctions = scheduledAuctions;
    }

    public long getActiveAuctions() {
        return activeAuctions;
    }

    public void setActiveAuctions(long activeAuctions) {
        this.activeAuctions = activeAuctions;
    }

    public long getClosedAuctions() {
        return closedAuctions;
    }

    public void setClosedAuctions(long closedAuctions) {
        this.closedAuctions = closedAuctions;
    }

    public long getTotalBids() {
        return totalBids;
    }

    public void setTotalBids(long totalBids) {
        this.totalBids = totalBids;
    }

    public long getCompletedAuctionsWithWinner() {
        return completedAuctionsWithWinner;
    }

    public void setCompletedAuctionsWithWinner(long completedAuctionsWithWinner) {
        this.completedAuctionsWithWinner = completedAuctionsWithWinner;
    }

    public long getCancelledAuctions() {
        return cancelledAuctions;
    }

    public void setCancelledAuctions(long cancelledAuctions) {
        this.cancelledAuctions = cancelledAuctions;
    }

    public double getTotalBidValue() {
        return totalBidValue;
    }

    public void setTotalBidValue(double totalBidValue) {
        this.totalBidValue = totalBidValue;
    }

    public double getSettledValue() {
        return settledValue;
    }

    public void setSettledValue(double settledValue) {
        this.settledValue = settledValue;
    }

    public double getHighestWinningBid() {
        return highestWinningBid;
    }

    public void setHighestWinningBid(double highestWinningBid) {
        this.highestWinningBid = highestWinningBid;
    }

    public long getNewUsersLast30Days() {
        return newUsersLast30Days;
    }

    public void setNewUsersLast30Days(long newUsersLast30Days) {
        this.newUsersLast30Days = newUsersLast30Days;
    }

    public long getNewUsersPrevious30Days() {
        return newUsersPrevious30Days;
    }

    public void setNewUsersPrevious30Days(long newUsersPrevious30Days) {
        this.newUsersPrevious30Days = newUsersPrevious30Days;
    }
}
