package com.ihub.dto;

import java.time.LocalDateTime;

/**
 * A decided auction, joined with its idea, creator and winner.
 *
 * <p>Replaces the admin console's previous approach of listing auctions and then
 * issuing one winner lookup per row.</p>
 */
public class AuctionWinnerSummaryResponse {

    private Long auctionId;
    private Long ideaId;
    private String ideaTitle;
    private String ideaCategory;
    private Long creatorId;
    private String creatorName;
    private Long winnerId;
    private String winnerName;
    private String winnerEmail;
    private Double winningBid;
    private int bidCount;
    private LocalDateTime endTime;
    private LocalDateTime decidedAt;

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

    public String getIdeaCategory() {
        return ideaCategory;
    }

    public void setIdeaCategory(String ideaCategory) {
        this.ideaCategory = ideaCategory;
    }

    public Long getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(Long creatorId) {
        this.creatorId = creatorId;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public Long getWinnerId() {
        return winnerId;
    }

    public void setWinnerId(Long winnerId) {
        this.winnerId = winnerId;
    }

    public String getWinnerName() {
        return winnerName;
    }

    public void setWinnerName(String winnerName) {
        this.winnerName = winnerName;
    }

    public String getWinnerEmail() {
        return winnerEmail;
    }

    public void setWinnerEmail(String winnerEmail) {
        this.winnerEmail = winnerEmail;
    }

    public Double getWinningBid() {
        return winningBid;
    }

    public void setWinningBid(Double winningBid) {
        this.winningBid = winningBid;
    }

    public int getBidCount() {
        return bidCount;
    }

    public void setBidCount(int bidCount) {
        this.bidCount = bidCount;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public LocalDateTime getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(LocalDateTime decidedAt) {
        this.decidedAt = decidedAt;
    }
}
