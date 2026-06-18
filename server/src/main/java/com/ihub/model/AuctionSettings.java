package com.ihub.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AuctionSettings {
    private Long id;
    private Long ideaId;
    private BigDecimal minBid;
    private BigDecimal reservePrice;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public AuctionSettings() {}
    
    public AuctionSettings(Long ideaId, BigDecimal minBid, BigDecimal reservePrice, String description) {
        this.ideaId = ideaId;
        this.minBid = minBid;
        this.reservePrice = reservePrice;
        this.description = description;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
