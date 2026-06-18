package com.ihub.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminAuditLogResponse {
    private Long id;
    private Long adminId;
    private String adminName;
    private String adminEmail;
    private String action;
    private String entityType;
    private Long entityId;
    private String details;
    private String ipAddress;
    private LocalDateTime createdAt;
}
