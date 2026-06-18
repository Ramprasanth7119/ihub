package com.ihub.dto;

import java.util.List;

public class IdeaResponse {

    private Long id;
    private Long creatorId;
    private String title;
    private String description;
    private String category;
    private Double basePrice;
    private Double maxBudget;
    private String status;
    private List<String> tags;

    public IdeaResponse() {
    }

    public IdeaResponse(Long id, Long creatorId, String title, String description, String category,
                        Double basePrice, Double maxBudget, String status, List<String> tags) {
        this.id = id;
        this.creatorId = creatorId;
        this.title = title;
        this.description = description;
        this.category = category;
        this.basePrice = basePrice;
        this.maxBudget = maxBudget;
        this.status = status;
        this.tags = tags;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(Long creatorId) {
        this.creatorId = creatorId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Double getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(Double basePrice) {
        this.basePrice = basePrice;
    }

    public Double getMaxBudget() {
        return maxBudget;
    }

    public void setMaxBudget(Double maxBudget) {
        this.maxBudget = maxBudget;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}
