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
}
