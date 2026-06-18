package com.ihub.dto;

import java.util.List;

public class AdminDashboardResponse {

    private PlatformMetricsResponse metrics;
    private List<AdminAuctionSummaryResponse> recentAuctions;
    private List<LeaderStatResponse> topCreators;
    private List<LeaderStatResponse> topInvestors;

    public PlatformMetricsResponse getMetrics() {
        return metrics;
    }

    public void setMetrics(PlatformMetricsResponse metrics) {
        this.metrics = metrics;
    }

    public List<AdminAuctionSummaryResponse> getRecentAuctions() {
        return recentAuctions;
    }

    public void setRecentAuctions(List<AdminAuctionSummaryResponse> recentAuctions) {
        this.recentAuctions = recentAuctions;
    }

    public List<LeaderStatResponse> getTopCreators() {
        return topCreators;
    }

    public void setTopCreators(List<LeaderStatResponse> topCreators) {
        this.topCreators = topCreators;
    }

    public List<LeaderStatResponse> getTopInvestors() {
        return topInvestors;
    }

    public void setTopInvestors(List<LeaderStatResponse> topInvestors) {
        this.topInvestors = topInvestors;
    }
}
