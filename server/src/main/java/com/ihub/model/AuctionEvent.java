package com.ihub.model;

import java.time.LocalDateTime;

public class AuctionEvent {

    private Long id;
    private Long auctionId;
    private String eventType;
    private String details;
    private LocalDateTime createdAt;

    public AuctionEvent() {
    }

    public AuctionEvent(Long id, Long auctionId, String eventType, String details, LocalDateTime createdAt) {
        this.id = id;
        this.auctionId = auctionId;
        this.eventType = eventType;
        this.details = details;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(Long auctionId) {
        this.auctionId = auctionId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
