package com.ihub.schedular;

import com.ihub.service.AuctionLifecycleService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AuctionScheduler {

    private final AuctionLifecycleService lifecycleService;

    public AuctionScheduler(AuctionLifecycleService lifecycleService) {
        this.lifecycleService = lifecycleService;
    }

    /**
     * Polls every minute for auctions due to start or close based on start_time / end_time.
     */
    @Scheduled(cron = "${auction.cron.lifecycle:0 */1 * * * *}")
    public void processAuctionLifecycle() {
        lifecycleService.processDueAuctions();
    }
}
