package com.ihub.service;

import com.ihub.dao.AuctionLifecycleDao;
import com.ihub.model.Auction;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Handles auction lifecycle:
 * - Activate auctions
 * - Close auctions
 * - Sync with Elasticsearch
 */
@Service
@RequiredArgsConstructor
public class AuctionLifecycleService {

    private final AuctionLifecycleDao lifecycleDao;
    private final IdeaSearchService ideaSearchService; // ✅ NEW
    
    

    public AuctionLifecycleService(AuctionLifecycleDao lifecycleDao, IdeaSearchService ideaSearchService) {
		super();
		this.lifecycleDao = lifecycleDao;
		this.ideaSearchService = ideaSearchService;
	}

	/**
     * Activate scheduled auctions
     */
    @Transactional
    public void activateAuctions() {

        // ✅ Step 1: Get auctions BEFORE update
        List<Auction> auctions = lifecycleDao.getScheduledAuctions();

        // ✅ Step 2: Update DB
        lifecycleDao.activateAuctions();

        // ✅ Step 3: Sync Elasticsearch
        for (Auction auction : auctions) {
            ideaSearchService.updateStatus(
                    auction.getIdeaId(),
                    "ACTIVE"
            );
        }

        System.out.println("✅ Auctions activated: " + auctions.size());
    }

    /**
     * Close auctions and determine winners
     */
    @Transactional
    public void closeAuctions() {

        // ✅ Step 1: Get active auctions BEFORE closing
        List<Auction> auctions = lifecycleDao.getActiveAuctions();

        // ✅ Step 2: Get winners
        List<Map<String, Object>> winners = lifecycleDao.getWinners();

        // ✅ Step 3: Save winners
        for (Map<String, Object> winner : winners) {

            lifecycleDao.saveWinner(
                    ((Number) winner.get("auction_id")).longValue(),
                    ((Number) winner.get("investor_id")).longValue(),
                    ((Number) winner.get("bid_amount")).doubleValue()
            );
        }

        // ✅ Step 4: Close auctions in DB
        lifecycleDao.closeAuctions();

        // ✅ Step 5: Sync Elasticsearch
        for (Auction auction : auctions) {
            ideaSearchService.updateStatus(
                    auction.getIdeaId(),
                    "CLOSED"
            );
        }

        System.out.println("🏆 Auctions closed & winners selected: " + auctions.size());
    }
}