package com.ihub.dto;

import java.time.LocalDateTime;

public class AuctionHistoryResponse {

    private Long id;
    private String eventType;
    private String details;
    private LocalDateTime createdAt;

    public AuctionHistoryResponse() {
    }

    public AuctionHistoryResponse(Long id, String eventType, String details, LocalDateTime createdAt) {
        this.id = id;
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
