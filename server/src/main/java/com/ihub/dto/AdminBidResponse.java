package com.ihub.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminBidResponse {
    private Long id;
    private Long auctionId;
    private String ideaTitle;
    private Long investorId;
    private String investorName;
    private String investorEmail;
    private Double bidAmount;
    private LocalDateTime createdAt;
    private String auctionStatus;
    private Integer rank;
}
