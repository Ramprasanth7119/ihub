package com.ihub.dto;

import lombok.Data;

/**
 * DTO for creating an idea
 */
@Data
public class IdeaRequest {

    private Long creatorId;
    private String title;
    private String description;
    private String category;
    private Double basePrice;
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
	public IdeaRequest(Long creatorId, String title, String description, String category, Double basePrice) {
		super();
		this.creatorId = creatorId;
		this.title = title;
		this.description = description;
		this.category = category;
		this.basePrice = basePrice;
	}
	public IdeaRequest() {
		super();
	}
}