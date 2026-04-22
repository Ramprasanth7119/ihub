package com.ihub.search;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

@Document(indexName = "ideas")
public class IdeaDocument {

    @Id
    private Long id;

    private String title;
    private String description;
    private String category;

    private Double minBudget;
    private Double maxBudget;

    private String auctionStatus; // ACTIVE / CLOSED

	public IdeaDocument(Long id, String title, String description, String category, Double minBudget, Double maxBudget,
			String auctionStatus) {
		super();
		this.id = id;
		this.title = title;
		this.description = description;
		this.category = category;
		this.minBudget = minBudget;
		this.maxBudget = maxBudget;
		this.auctionStatus = auctionStatus;
	}

	public IdeaDocument() {
		super();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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

	public Double getMinBudget() {
		return minBudget;
	}

	public void setMinBudget(Double minBudget) {
		this.minBudget = minBudget;
	}

	public Double getMaxBudget() {
		return maxBudget;
	}

	public void setMaxBudget(Double maxBudget) {
		this.maxBudget = maxBudget;
	}

	public String getAuctionStatus() {
		return auctionStatus;
	}

	public void setAuctionStatus(String auctionStatus) {
		this.auctionStatus = auctionStatus;
	}
    
    
}