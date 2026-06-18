package com.ihub.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UpdateAuctionRequest {
    @Future(message = "Start time must be in the future")
    private LocalDateTime startTime;
    
    @Future(message = "End time must be in the future")
    private LocalDateTime endTime;
    
    @DecimalMin(value = "0.01", message = "Minimum bid must be at least 0.01")
    private Double minBidIncrement;
    
    private String description;
}
