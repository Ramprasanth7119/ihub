package com.ihub.dto;

public class BidResponse {

    private String message;
    private Long bidId;
    private Long auctionId;
    private Double amount;
    private Double currentHighest;
    private Integer rank;

    public BidResponse() {
    }

    public BidResponse(String message, Long bidId, Long auctionId, Double amount,
                       Double currentHighest, Integer rank) {
        this.message = message;
        this.bidId = bidId;
        this.auctionId = auctionId;
        this.amount = amount;
        this.currentHighest = currentHighest;
        this.rank = rank;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getBidId() {
        return bidId;
    }

    public void setBidId(Long bidId) {
        this.bidId = bidId;
    }

    public Long getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(Long auctionId) {
        this.auctionId = auctionId;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public Double getCurrentHighest() {
        return currentHighest;
    }

    public void setCurrentHighest(Double currentHighest) {
        this.currentHighest = currentHighest;
    }

    public Integer getRank() {
        return rank;
    }

    public void setRank(Integer rank) {
        this.rank = rank;
    }
}
