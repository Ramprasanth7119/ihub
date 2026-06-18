package com.ihub.dto;

import java.time.LocalDateTime;

public class LeaderboardEntryResponse {

    private int rank;
    private Long investorId;
    private String investorName;
    private Double highestBid;
    private LocalDateTime lastBidAt;

    public LeaderboardEntryResponse() {
    }

    public LeaderboardEntryResponse(int rank, Long investorId, String investorName,
                                    Double highestBid, LocalDateTime lastBidAt) {
        this.rank = rank;
        this.investorId = investorId;
        this.investorName = investorName;
        this.highestBid = highestBid;
        this.lastBidAt = lastBidAt;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public Long getInvestorId() {
        return investorId;
    }

    public void setInvestorId(Long investorId) {
        this.investorId = investorId;
    }

    public String getInvestorName() {
        return investorName;
    }

    public void setInvestorName(String investorName) {
        this.investorName = investorName;
    }

    public Double getHighestBid() {
        return highestBid;
    }

    public void setHighestBid(Double highestBid) {
        this.highestBid = highestBid;
    }

    public LocalDateTime getLastBidAt() {
        return lastBidAt;
    }

    public void setLastBidAt(LocalDateTime lastBidAt) {
        this.lastBidAt = lastBidAt;
    }
}
