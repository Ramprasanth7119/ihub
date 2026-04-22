package com.ihub.model;

import lombok.Data;

/**
 * Model representing Idea entity
 */
@Data
public class Idea {

    private Long id;
    private Long creatorId;
    private String title;
    private String description;
    private String category;
    private Double basePrice;
    private String status;
	public Idea(Long id, Long creatorId, String title, String description, String category, Double basePrice,
			String status) {
		super();
		this.id = id;
		this.creatorId = creatorId;
		this.title = title;
		this.description = description;
		this.category = category;
		this.basePrice = basePrice;
		this.status = status;
	}
	public Idea() {
		super();
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
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
    
    
}