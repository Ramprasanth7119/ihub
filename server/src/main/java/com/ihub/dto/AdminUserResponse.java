package com.ihub.dto;

import java.time.LocalDateTime;

public class AdminUserResponse {

    private Long id;
    private String name;
    private String email;
    private String role;
    private boolean verified;
    private boolean active;
    private LocalDateTime createdAt;
    private long ideaCount;
    private long bidCount;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public long getIdeaCount() {
        return ideaCount;
    }

    public void setIdeaCount(long ideaCount) {
        this.ideaCount = ideaCount;
    }

    public long getBidCount() {
        return bidCount;
    }

    public void setBidCount(long bidCount) {
        this.bidCount = bidCount;
    }
}
