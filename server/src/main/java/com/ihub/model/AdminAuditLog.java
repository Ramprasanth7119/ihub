package com.ihub.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminAuditLog {
    private Long id;
    private Long adminId;
    private String adminName;
    private String action;
    private String entityType;
    private Long entityId;
    private String details;
    private String ipAddress;
    private LocalDateTime createdAt;
    
    public AdminAuditLog() {}
    
    public AdminAuditLog(Long adminId, String adminName, String action, String entityType, Long entityId, String details, String ipAddress) {
        this.adminId = adminId;
        this.adminName = adminName;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.details = details;
        this.ipAddress = ipAddress;
        this.createdAt = LocalDateTime.now();
    }
}
