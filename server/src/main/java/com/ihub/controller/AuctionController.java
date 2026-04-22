package com.ihub.controller;

import com.ihub.dto.AuctionRequest;
import com.ihub.dto.AuctionResponse;
import com.ihub.service.AuctionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Auction APIs
 */
@RestController
@RequestMapping("/api/auctions")
@RequiredArgsConstructor
public class AuctionController {

    private final AuctionService auctionService;

    
    public AuctionController(AuctionService auctionService) {
		super();
		this.auctionService = auctionService;
	}

	@PostMapping
    public AuctionResponse createAuction(@RequestBody AuctionRequest request) {
        return auctionService.createAuction(request);
    }

    @GetMapping("/{id}")
    public AuctionResponse getAuction(@PathVariable Long id) {
        return auctionService.getAuction(id);
    }

    @GetMapping
    public List<AuctionResponse> getAllAuctions() {
        return auctionService.getAllAuctions();
    }
}