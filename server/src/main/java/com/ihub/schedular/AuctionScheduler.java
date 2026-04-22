package com.ihub.schedular;

import com.ihub.service.AuctionLifecycleService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Handles scheduled auction lifecycle events
 */
@Component
@RequiredArgsConstructor
public class AuctionScheduler {

    private final AuctionLifecycleService lifecycleService;

    public AuctionScheduler(AuctionLifecycleService lifecycleService) {
		super();
		this.lifecycleService = lifecycleService;
	}

	/**
     * Every Friday at 6 PM → Activate auctions
     */
    @Scheduled(cron = "0 0 18 ? * FRI")
    public void startAuctions() {
        lifecycleService.activateAuctions();
    }

    /**
     * Every Friday at 11 PM → Close auctions
     */
    @Scheduled(cron = "0 0 23 ? * FRI")
    public void closeAuctions() {
        lifecycleService.closeAuctions();
    }
}