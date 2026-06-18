package com.ihub.dto;

import java.time.LocalDateTime;

public class BidHistoryResponse {

    private Long bidId;
    private Long investorId;
    private String investorName;
    private Double amount;
    private LocalDateTime placedAt;

    public BidHistoryResponse() {
    }

    public BidHistoryResponse(Long bidId, Long investorId, String investorName, Double amount, LocalDateTime placedAt) {
        this.bidId = bidId;
        this.investorId = investorId;
        this.investorName = investorName;
        this.amount = amount;
        this.placedAt = placedAt;
    }

    public Long getBidId() {
        return bidId;
    }

    public void setBidId(Long bidId) {
        this.bidId = bidId;
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

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public LocalDateTime getPlacedAt() {
        return placedAt;
    }

    public void setPlacedAt(LocalDateTime placedAt) {
        this.placedAt = placedAt;
    }
}
