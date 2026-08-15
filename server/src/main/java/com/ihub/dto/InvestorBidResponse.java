package com.ihub.dto;

import java.time.LocalDateTime;

/**
 * One row of an investor's own bidding history, denormalised with the idea and
 * auction context so the client can render "My Bids" without a follow-up request
 * per auction.
 */
public class InvestorBidResponse {

    private Long bidId;
    private Long auctionId;
    private Long ideaId;
    private String ideaTitle;
    private Double amount;
    private Double highestBid;
    private String auctionStatus;
    private LocalDateTime endTime;
    private LocalDateTime placedAt;
    private boolean leading;
    private boolean won;

    public InvestorBidResponse() {
    }

    public Long getBidId() {
        return bidId;
    }

    public void setBidId(Long bidId) {
        this.bidId = bidId;
    }

    public Long getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(Long auctionId) {
        this.auctionId = auctionId;
    }

    public Long getIdeaId() {
        return ideaId;
    }

    public void setIdeaId(Long ideaId) {
        this.ideaId = ideaId;
    }

    public String getIdeaTitle() {
        return ideaTitle;
    }

    public void setIdeaTitle(String ideaTitle) {
        this.ideaTitle = ideaTitle;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public Double getHighestBid() {
        return highestBid;
    }

    public void setHighestBid(Double highestBid) {
        this.highestBid = highestBid;
    }

    public String getAuctionStatus() {
        return auctionStatus;
    }

    public void setAuctionStatus(String auctionStatus) {
        this.auctionStatus = auctionStatus;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public LocalDateTime getPlacedAt() {
        return placedAt;
    }

    public void setPlacedAt(LocalDateTime placedAt) {
        this.placedAt = placedAt;
    }

    /** True when this bid is currently the highest on its auction. */
    public boolean isLeading() {
        return leading;
    }

    public void setLeading(boolean leading) {
        this.leading = leading;
    }

    /** True when the auction closed and this investor was recorded as the winner. */
    public boolean isWon() {
        return won;
    }

    public void setWon(boolean won) {
        this.won = won;
    }
}
