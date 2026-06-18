package com.ihub.service;

import com.ihub.dto.BidUpdate;
import com.ihub.dto.LeaderboardEntryResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BidBroadcastService {

    private final SimpMessagingTemplate messagingTemplate;

    public BidBroadcastService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcastBidUpdate(BidUpdate update) {
        messagingTemplate.convertAndSend(
                "/topic/auction/" + update.getAuctionId() + "/bids",
                update
        );
    }

    public void broadcastLeaderboard(Long auctionId, List<LeaderboardEntryResponse> leaderboard) {
        messagingTemplate.convertAndSend(
                "/topic/auction/" + auctionId + "/leaderboard",
                leaderboard
        );
    }
}
