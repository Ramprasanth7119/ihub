package com.ihub.dto;

public class LeaderStatResponse {

    private Long userId;
    private String name;
    private String email;
    private long count;

    public LeaderStatResponse() {
    }

    public LeaderStatResponse(Long userId, String name, String email, long count) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.count = count;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}
