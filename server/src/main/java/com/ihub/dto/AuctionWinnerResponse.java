package com.ihub.dto;

import java.time.LocalDateTime;

public class AuctionWinnerResponse {

    private Long auctionId;
    private Long winnerId;
    private String winnerName;
    private Double winningBid;
    private LocalDateTime selectedAt;

    public AuctionWinnerResponse() {
    }

    public AuctionWinnerResponse(Long auctionId, Long winnerId, String winnerName,
                                 Double winningBid, LocalDateTime selectedAt) {
        this.auctionId = auctionId;
        this.winnerId = winnerId;
        this.winnerName = winnerName;
        this.winningBid = winningBid;
        this.selectedAt = selectedAt;
    }

    public Long getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(Long auctionId) {
        this.auctionId = auctionId;
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

    public Double getWinningBid() {
        return winningBid;
    }

    public void setWinningBid(Double winningBid) {
        this.winningBid = winningBid;
    }

    public LocalDateTime getSelectedAt() {
        return selectedAt;
    }

    public void setSelectedAt(LocalDateTime selectedAt) {
        this.selectedAt = selectedAt;
    }
}
