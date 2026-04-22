package com.ihub.dto;

import lombok.Data;

/**
 * Request for placing a bid
 */
@Data
public class BidRequest {

    private Long auctionId;
    private Double amount;
	public BidRequest(Long auctionId, Double amount) {
		super();
		this.auctionId = auctionId;
		this.amount = amount;
	}
	public BidRequest() {
		super();
	}
	public Long getAuctionId() {
		return auctionId;
	}
	public void setAuctionId(Long auctionId) {
		this.auctionId = auctionId;
	}
	public Double getAmount() {
		return amount;
	}
	public void setAmount(Double amount) {
		this.amount = amount;
	}
    
	
}