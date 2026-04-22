package com.ihub.dto;

import java.time.LocalDateTime;

/**
 * WebSocket payload for live bidding
 */
public class BidUpdate {

    private Long auctionId;
    private Long investorId;
    private Double amount;
    private Integer rank;
    private LocalDateTime timestamp;
	public BidUpdate(Long auctionId, Long investorId, Double amount, Integer rank, LocalDateTime timestamp) {
		super();
		this.auctionId = auctionId;
		this.investorId = investorId;
		this.amount = amount;
		this.rank = rank;
		this.timestamp = timestamp;
	}
	public BidUpdate() {
		super();
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
	public Double getAmount() {
		return amount;
	}
	public void setAmount(Double amount) {
		this.amount = amount;
	}
	public Integer getRank() {
		return rank;
	}
	public void setRank(Integer rank) {
		this.rank = rank;
	}
	public LocalDateTime getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(LocalDateTime timestamp) {
		this.timestamp = timestamp;
	}
}