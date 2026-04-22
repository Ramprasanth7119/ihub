package com.ihub.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Bid {

    private Long id;
    private Long auctionId;
    private Long investorId;
    private Double bidAmount;
    private LocalDateTime createdAt;
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Long getAuctionId() {
		return auctionId;
	}
	public void setAuctionId(Long auctionId) {
		this.auctionId = auctionId;
	}
	public Long getInvestorId() {
		return investorId;
	}
	public void setInvestorId(Long investorId) {
		this.investorId = investorId;
	}
	public Double getBidAmount() {
		return bidAmount;
	}
	public void setBidAmount(Double bidAmount) {
		this.bidAmount = bidAmount;
	}
	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}
	public Bid(Long id, Long auctionId, Long investorId, Double bidAmount, LocalDateTime createdAt) {
		super();
		this.id = id;
		this.auctionId = auctionId;
		this.investorId = investorId;
		this.bidAmount = bidAmount;
		this.createdAt = createdAt;
	}
	public Bid() {
		super();
	}
    
    
}