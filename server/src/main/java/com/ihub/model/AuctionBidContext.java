package com.ihub.model;

public class AuctionBidContext {

    private String status;
    private Double minBidIncrement;
    private Double basePrice;
    private Long ideaCreatorId;

    public AuctionBidContext() {
    }

    public AuctionBidContext(String status, Double minBidIncrement, Double basePrice, Long ideaCreatorId) {
        this.status = status;
        this.minBidIncrement = minBidIncrement;
        this.basePrice = basePrice;
        this.ideaCreatorId = ideaCreatorId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getMinBidIncrement() {
        return minBidIncrement;
    }

    public void setMinBidIncrement(Double minBidIncrement) {
        this.minBidIncrement = minBidIncrement;
    }

    public Double getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(Double basePrice) {
        this.basePrice = basePrice;
    }

    public Long getIdeaCreatorId() {
        return ideaCreatorId;
    }

    public void setIdeaCreatorId(Long ideaCreatorId) {
        this.ideaCreatorId = ideaCreatorId;
    }
}
