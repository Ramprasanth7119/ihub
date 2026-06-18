package com.ihub.controller;

import com.ihub.dto.BidHistoryResponse;
import com.ihub.dto.BidRequest;
import com.ihub.dto.BidResponse;
import com.ihub.dto.HighestBidResponse;
import com.ihub.dto.LeaderboardEntryResponse;
import com.ihub.service.BidService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bids")
public class BidController {

    private final BidService bidService;

    public BidController(BidService bidService) {
        this.bidService = bidService;
    }

    @PostMapping
    public BidResponse placeBid(@Valid @RequestBody BidRequest request) {
        return bidService.placeBid(request);
    }

    @GetMapping("/auction/{auctionId}/history")
    public List<BidHistoryResponse> getBidHistory(@PathVariable Long auctionId) {
        return bidService.getBidHistory(auctionId);
    }

    @GetMapping("/auction/{auctionId}/highest")
    public HighestBidResponse getHighestBid(@PathVariable Long auctionId) {
        return bidService.getHighestBid(auctionId);
    }

    @GetMapping("/auction/{auctionId}/leaderboard")
    public List<LeaderboardEntryResponse> getLeaderboard(@PathVariable Long auctionId) {
        return bidService.getLeaderboard(auctionId);
    }
}
