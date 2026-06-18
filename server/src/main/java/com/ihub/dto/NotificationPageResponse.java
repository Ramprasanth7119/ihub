package com.ihub.dto;

import java.util.List;

public class NotificationPageResponse {

    private List<NotificationResponse> content;
    private long totalElements;
    private long unreadCount;
    private int page;
    private int size;

    public NotificationPageResponse() {
    }

    public NotificationPageResponse(List<NotificationResponse> content, long totalElements,
                                  long unreadCount, int page, int size) {
        this.content = content;
        this.totalElements = totalElements;
        this.unreadCount = unreadCount;
        this.page = page;
        this.size = size;
    }

    public List<NotificationResponse> getContent() {
        return content;
    }

    public void setContent(List<NotificationResponse> content) {
        this.content = content;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public long getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(long unreadCount) {
        this.unreadCount = unreadCount;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
