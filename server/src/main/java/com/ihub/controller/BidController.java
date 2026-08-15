package com.ihub.controller;

import com.ihub.dto.BidHistoryResponse;
import com.ihub.dto.BidRequest;
import com.ihub.dto.BidResponse;
import com.ihub.dto.HighestBidResponse;
import com.ihub.dto.InvestorBidResponse;
import com.ihub.dto.LeaderboardEntryResponse;
import com.ihub.service.BidService;
import com.ihub.util.PagedResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bids")
public class BidController {

    private final BidService bidService;

    public BidController(BidService bidService) {
        this.bidService = bidService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BidResponse placeBid(@Valid @RequestBody BidRequest request) {
        return bidService.placeBid(request);
    }

    @GetMapping("/auction/{auctionId}/history")
    public ResponseEntity<List<BidHistoryResponse>> getBidHistory(
            @PathVariable Long auctionId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return PagedResponse.of(bidService.getBidHistory(auctionId, page, size));
    }

    @GetMapping("/auction/{auctionId}/highest")
    public HighestBidResponse getHighestBid(@PathVariable Long auctionId) {
        return bidService.getHighestBid(auctionId);
    }

    @GetMapping("/auction/{auctionId}/leaderboard")
    public List<LeaderboardEntryResponse> getLeaderboard(@PathVariable Long auctionId) {
        return bidService.getLeaderboard(auctionId);
    }

    /** Distinct bidder count, shown alongside the countdown on the live auction page. */
    @GetMapping("/auction/{auctionId}/bidders/count")
    public Map<String, Integer> getBidderCount(@PathVariable Long auctionId) {
        return Map.of("count", bidService.getBidderCount(auctionId));
    }

    /** The authenticated investor's own bid history. */
    @GetMapping("/my")
    public ResponseEntity<List<InvestorBidResponse>> getMyBids(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return PagedResponse.of(bidService.getMyBids(page, size));
    }
}
