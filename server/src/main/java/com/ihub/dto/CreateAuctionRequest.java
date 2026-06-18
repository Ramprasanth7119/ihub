package com.ihub.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateAuctionRequest {
    @NotNull(message = "Idea ID is required")
    private Long ideaId;
    
    @NotNull(message = "Start time is required")
    @Future(message = "Start time must be in the future")
    private LocalDateTime startTime;
    
    @NotNull(message = "End time is required")
    @Future(message = "End time must be in the future")
    private LocalDateTime endTime;
    
    @DecimalMin(value = "0.01", message = "Minimum bid must be at least 0.01")
    private Double minBid;
    
    @DecimalMin(value = "0.01", message = "Reserve price must be at least 0.01")
    private Double reservePrice;
    
    private String description;
}
