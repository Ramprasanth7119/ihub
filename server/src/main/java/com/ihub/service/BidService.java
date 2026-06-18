package com.ihub.service;

import com.ihub.dao.BidDao;
import com.ihub.dao.UserDao;
import com.ihub.dto.BidHistoryResponse;
import com.ihub.dto.BidRequest;
import com.ihub.dto.BidResponse;
import com.ihub.dto.BidUpdate;
import com.ihub.dto.HighestBidResponse;
import com.ihub.dto.LeaderboardEntryResponse;
import com.ihub.exception.CustomException;
import com.ihub.model.AuctionBidContext;
import com.ihub.model.User;
import com.ihub.notification.event.OutbidEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class BidService {

    private final BidDao bidDao;
    private final UserDao userDao;
    private final BidBroadcastService broadcastService;
    private final ApplicationEventPublisher eventPublisher;

    public BidService(
            BidDao bidDao,
            UserDao userDao,
            BidBroadcastService broadcastService,
            ApplicationEventPublisher eventPublisher) {
        this.bidDao = bidDao;
        this.userDao = userDao;
        this.broadcastService = broadcastService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public BidResponse placeBid(BidRequest request) {
        User investor = getAuthenticatedInvestor();
        Long auctionId = request.getAuctionId();

        AuctionBidContext context = bidDao.lockAuctionForBid(auctionId);
        if (context == null) {
            throw new CustomException("Auction not found");
        }

        if (!"ACTIVE".equalsIgnoreCase(context.getStatus())) {
            throw new CustomException("Auction is not active");
        }

        if (investor.getId().equals(context.getIdeaCreatorId())) {
            throw new CustomException("Creators cannot bid on their own ideas");
        }

        Long previousLeaderId = bidDao.findLeaderInvestorId(auctionId);
        Double currentHighest = bidDao.getHighestBid(auctionId);
        double minimumRequired = calculateMinimumBid(context, currentHighest);

        if (request.getAmount() < minimumRequired) {
            throw new CustomException(
                    String.format("Bid must be at least %.2f (current highest: %s, increment: %.2f)",
                            minimumRequired,
                            currentHighest != null ? String.format("%.2f", currentHighest) : "none",
                            context.getMinBidIncrement())
            );
        }

        Long bidId = bidDao.saveBid(auctionId, investor.getId(), request.getAmount());

        if (previousLeaderId != null && !previousLeaderId.equals(investor.getId())) {
            String ideaTitle = bidDao.findIdeaTitleByAuction(auctionId);
            eventPublisher.publishEvent(new OutbidEvent(
                    previousLeaderId,
                    auctionId,
                    ideaTitle,
                    request.getAmount()
            ));
        }

        List<LeaderboardEntryResponse> leaderboard = getLeaderboard(auctionId);
        int rank = resolveRank(leaderboard, investor.getId());

        BidUpdate update = new BidUpdate(
                auctionId,
                investor.getId(),
                request.getAmount(),
                rank,
                LocalDateTime.now()
        );
        broadcastService.broadcastBidUpdate(update);
        broadcastService.broadcastLeaderboard(auctionId, leaderboard);

        return new BidResponse(
                "Bid placed successfully",
                bidId,
                auctionId,
                request.getAmount(),
                request.getAmount(),
                rank
        );
    }

    public List<BidHistoryResponse> getBidHistory(Long auctionId) {
        return bidDao.findBidHistory(auctionId).stream()
                .map(row -> new BidHistoryResponse(
                        ((Number) row.get("bid_id")).longValue(),
                        ((Number) row.get("investor_id")).longValue(),
                        (String) row.get("investor_name"),
                        ((Number) row.get("bid_amount")).doubleValue(),
                        toLocalDateTime(row.get("created_at"))
                ))
                .toList();
    }

    public HighestBidResponse getHighestBid(Long auctionId) {
        Map<String, Object> row = bidDao.findHighestBid(auctionId);
        if (row == null) {
            throw new CustomException("No bids placed yet for this auction");
        }

        return new HighestBidResponse(
                auctionId,
                ((Number) row.get("bid_id")).longValue(),
                ((Number) row.get("investor_id")).longValue(),
                (String) row.get("investor_name"),
                ((Number) row.get("bid_amount")).doubleValue(),
                toLocalDateTime(row.get("created_at"))
        );
    }

    public List<LeaderboardEntryResponse> getLeaderboard(Long auctionId) {
        List<Map<String, Object>> rows = bidDao.findLeaderboard(auctionId);
        List<LeaderboardEntryResponse> leaderboard = new ArrayList<>();

        int rank = 1;
        for (Map<String, Object> row : rows) {
            leaderboard.add(new LeaderboardEntryResponse(
                    rank++,
                    ((Number) row.get("investor_id")).longValue(),
                    (String) row.get("investor_name"),
                    ((Number) row.get("bid_amount")).doubleValue(),
                    toLocalDateTime(row.get("latest_bid_at"))
            ));
        }
        return leaderboard;
    }

    private double calculateMinimumBid(AuctionBidContext context, Double currentHighest) {
        if (currentHighest == null) {
            return context.getBasePrice();
        }
        return currentHighest + context.getMinBidIncrement();
    }

    private int resolveRank(List<LeaderboardEntryResponse> leaderboard, Long investorId) {
        return leaderboard.stream()
                .filter(entry -> entry.getInvestorId().equals(investorId))
                .map(LeaderboardEntryResponse::getRank)
                .findFirst()
                .orElse(leaderboard.size() + 1);
    }

    private User getAuthenticatedInvestor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new CustomException("User not authenticated");
        }

        User user = userDao.findByEmail(auth.getName());
        if (user == null) {
            throw new CustomException("User not found");
        }
        if (!"INVESTOR".equalsIgnoreCase(user.getRole())) {
            throw new CustomException("Only investors can place bids");
        }
        return user;
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        if (value instanceof LocalDateTime dateTime) {
            return dateTime;
        }
        return null;
    }
}
