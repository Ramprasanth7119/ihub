package com.ihub.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminIdeaResponse {
    private Long id;
    private Long creatorId;
    private String creatorName;
    private String creatorEmail;
    private String title;
    private String description;
    private String category;
    private Double basePrice;
    private Double maxBudget;
    private String status;
    private LocalDateTime createdAt;
    private Long auctionCount;
    private Boolean hasActiveAuction;
}
