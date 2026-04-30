package com.ihub.service;

import java.util.List;
import java.util.Map;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.ihub.dto.BidUpdate;

/**
 * Sends real-time bid updates to clients
 */
@Service
public class BidBroadcastService {

    private final SimpMessagingTemplate messagingTemplate;

    public BidBroadcastService(SimpMessagingTemplate messagingTemplate) {
		super();
		this.messagingTemplate = messagingTemplate;
	}

	public void broadcastBid(Long auctionId, Double amount) {

        messagingTemplate.convertAndSend(
                "/topic/auction/" + auctionId,
                amount
        );
    }
	
	public void broadcastBidUpdate(BidUpdate update) {

	    messagingTemplate.convertAndSend(
	            "/topic/auction/" + update.getAuctionId(),
	            update
	    );
	}
	
	public void broadcastLeaderboard(Long auctionId, List<Map<String, Object>> leaderboard) {

	    messagingTemplate.convertAndSend(
	            "/topic/auction/" + auctionId,
	            leaderboard
	    );
	}
}