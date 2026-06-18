package com.ihub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class IdeaStatusUpdateRequest {
    @NotBlank(message = "Status is required")
    @Pattern(regexp = "PENDING|APPROVED|REJECTED|SUSPENDED|DRAFT|PUBLISHED|ARCHIVED", 
             message = "Invalid status. Must be PENDING, APPROVED, REJECTED, SUSPENDED, DRAFT, PUBLISHED, or ARCHIVED")
    private String status;
    
    private String reason;
}
