package com.ihub.model;

import java.time.LocalDateTime;

/**
 * Snapshot of the auction row taken under a {@code SELECT ... FOR UPDATE} lock when a
 * bid is placed. Carries the scheduled window as well as the status: the lifecycle
 * scheduler only runs once a minute, so status alone would let bids through after the
 * advertised end time.
 */
public class AuctionBidContext {

    private String status;
    private Double minBidIncrement;
    private Double basePrice;
    private Long ideaCreatorId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    /**
     * Window flags evaluated by the database against its own clock, so a JVM in a
     * different time zone cannot accept a bid the scheduler considers out of window.
     * The {@code startTime}/{@code endTime} values above are for messaging only.
     */
    private boolean beforeStart;
    private boolean afterEnd;

    public AuctionBidContext() {
    }

    public AuctionBidContext(String status, Double minBidIncrement, Double basePrice, Long ideaCreatorId) {
        this.status = status;
        this.minBidIncrement = minBidIncrement;
        this.basePrice = basePrice;
        this.ideaCreatorId = ideaCreatorId;
    }

    public boolean isBeforeStart() {
        return beforeStart;
    }

    public void setBeforeStart(boolean beforeStart) {
        this.beforeStart = beforeStart;
    }

    public boolean isAfterEnd() {
        return afterEnd;
    }

    public void setAfterEnd(boolean afterEnd) {
        this.afterEnd = afterEnd;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
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
