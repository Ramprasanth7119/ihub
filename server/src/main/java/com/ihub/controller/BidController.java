package com.ihub.controller;

import com.ihub.dto.BidRequest;
import com.ihub.dto.BidResponse;
import com.ihub.service.BidService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Bidding APIs
 */
@RestController
@RequestMapping("/api/bids")
@RequiredArgsConstructor
public class BidController {

    private final BidService bidService;
    
    

    public BidController(BidService bidService) {
		super();
		this.bidService = bidService;
	}



	@PostMapping
    public BidResponse placeBid(@RequestBody BidRequest request) {
        return bidService.placeBid(request);
    }
}