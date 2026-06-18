package com.ihub.controller;

import com.ihub.dto.AuctionHistoryResponse;
import com.ihub.dto.AuctionRequest;
import com.ihub.dto.AuctionResponse;
import com.ihub.dto.AuctionWinnerResponse;
import com.ihub.service.AuctionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auctions")
public class AuctionController {

    private final AuctionService auctionService;

    public AuctionController(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @PostMapping
    public AuctionResponse createAuction(@Valid @RequestBody AuctionRequest request) {
        return auctionService.createAuction(request);
    }

    @GetMapping("/{id}")
    public AuctionResponse getAuction(@PathVariable Long id) {
        return auctionService.getAuction(id);
    }

    @GetMapping
    public List<AuctionResponse> getAuctions(@RequestParam(required = false) String status) {
        return auctionService.getAuctions(status);
    }

    @GetMapping("/{id}/winner")
    public AuctionWinnerResponse getWinner(@PathVariable Long id) {
        return auctionService.getWinner(id);
    }

    @GetMapping("/{id}/history")
    public List<AuctionHistoryResponse> getHistory(@PathVariable Long id) {
        return auctionService.getHistory(id);
    }

    @PostMapping("/{id}/start")
    public AuctionResponse startAuction(@PathVariable Long id) {
        return auctionService.startAuction(id);
    }

    @PostMapping("/{id}/close")
    public AuctionResponse closeAuction(@PathVariable Long id) {
        return auctionService.closeAuction(id);
    }
}
