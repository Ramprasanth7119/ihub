package com.ihub.service;

import com.ihub.dao.BidDao;
import com.ihub.dao.UserDao;
import com.ihub.dto.BidHistoryResponse;
import com.ihub.dto.BidRequest;
import com.ihub.dto.BidResponse;
import com.ihub.dto.BidUpdate;
import com.ihub.dto.HighestBidResponse;
import com.ihub.dto.InvestorBidResponse;
import com.ihub.dto.LeaderboardEntryResponse;
import com.ihub.exception.ConflictException;
import com.ihub.exception.ForbiddenException;
import com.ihub.exception.NotFoundException;
import com.ihub.exception.UnauthorizedException;
import com.ihub.exception.UnprocessableEntityException;
import com.ihub.model.AuctionBidContext;
import com.ihub.model.PagedResult;
import com.ihub.model.User;
import com.ihub.notification.event.OutbidEvent;
import com.ihub.util.Pagination;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
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

    /**
     * Places a bid.
     *
     * <p>The auction row is locked for the duration of the transaction, so two
     * investors bidding at the same instant are serialised: the second one sees the
     * first one's amount when computing the minimum and is rejected if it no longer
     * clears the increment.</p>
     *
     * <p>{@code READ_COMMITTED} is required for that to hold. Under MySQL's default
     * {@code REPEATABLE READ}, the transaction's consistent snapshot is established
     * by the first plain read — which here is the user lookup below, taken
     * <em>before</em> the row lock is acquired. The subsequent
     * {@code getHighestBid()} is a non-locking read, so it would be served from that
     * stale snapshot and miss a bid committed by another transaction while this one
     * waited for the lock, letting two investors both win the same amount.</p>
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BidResponse placeBid(BidRequest request) {
        User investor = getAuthenticatedInvestor();
        Long auctionId = request.getAuctionId();

        AuctionBidContext context = bidDao.lockAuctionForBid(auctionId);
        if (context == null) {
            throw new NotFoundException("Auction not found");
        }

        assertAuctionAcceptsBids(context);

        if (investor.getId().equals(context.getIdeaCreatorId())) {
            throw new ForbiddenException("Creators cannot bid on their own ideas");
        }

        Long previousLeaderId = bidDao.findLeaderInvestorId(auctionId);
        Double currentHighest = bidDao.getHighestBid(auctionId);
        double minimumRequired = calculateMinimumBid(context, currentHighest);

        if (request.getAmount() < minimumRequired) {
            throw new UnprocessableEntityException(
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

    /**
     * Rejects bids outside the auction's live window.
     *
     * <p>Status alone is not sufficient: the lifecycle scheduler only sweeps once a
     * minute, so an auction can still read {@code ACTIVE} for up to a minute past its
     * advertised end time. The window flags come from the database's own clock — the
     * same one the scheduler uses — so the two can never disagree.</p>
     */
    private void assertAuctionAcceptsBids(AuctionBidContext context) {
        String status = context.getStatus();

        if ("CANCELLED".equalsIgnoreCase(status)) {
            throw new ConflictException("This auction has been cancelled");
        }
        if ("CLOSED".equalsIgnoreCase(status)) {
            throw new ConflictException("This auction has already closed");
        }
        if (!"ACTIVE".equalsIgnoreCase(status)) {
            throw new ConflictException("This auction has not started yet");
        }
        if (context.isBeforeStart()) {
            throw new ConflictException("This auction has not started yet");
        }
        if (context.isAfterEnd()) {
            throw new ConflictException("This auction has already closed");
        }
    }

    public PagedResult<BidHistoryResponse> getBidHistory(Long auctionId, Integer page, Integer size) {
        int resolvedPage = Pagination.resolvePage(page);
        int resolvedSize = Pagination.resolveSize(size, 50, Pagination.MAX_PAGE_SIZE);

        List<BidHistoryResponse> content = bidDao
                .findBidHistory(auctionId, resolvedSize, Pagination.offset(resolvedPage, resolvedSize))
                .stream()
                .map(row -> new BidHistoryResponse(
                        ((Number) row.get("bid_id")).longValue(),
                        ((Number) row.get("investor_id")).longValue(),
                        (String) row.get("investor_name"),
                        ((Number) row.get("bid_amount")).doubleValue(),
                        toLocalDateTime(row.get("created_at"))
                ))
                .toList();

        return new PagedResult<>(content, bidDao.countBidsForAuction(auctionId), resolvedPage, resolvedSize);
    }

    public HighestBidResponse getHighestBid(Long auctionId) {
        Map<String, Object> row = bidDao.findHighestBid(auctionId);
        if (row == null) {
            throw new NotFoundException("No bids placed yet for this auction");
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

    /** Number of distinct investors participating in an auction. */
    public int getBidderCount(Long auctionId) {
        return bidDao.countDistinctBidders(auctionId);
    }

    /** The authenticated investor's own bid history across all auctions. */
    public PagedResult<InvestorBidResponse> getMyBids(Integer page, Integer size) {
        User investor = getAuthenticatedInvestor();

        int resolvedPage = Pagination.resolvePage(page);
        int resolvedSize = Pagination.resolveSize(size);

        List<InvestorBidResponse> content = bidDao
                .findBidsByInvestor(investor.getId(), resolvedSize, Pagination.offset(resolvedPage, resolvedSize))
                .stream()
                .map(this::toInvestorBid)
                .toList();

        return new PagedResult<>(content, bidDao.countBidsByInvestor(investor.getId()), resolvedPage, resolvedSize);
    }

    private InvestorBidResponse toInvestorBid(Map<String, Object> row) {
        InvestorBidResponse response = new InvestorBidResponse();
        response.setBidId(((Number) row.get("bid_id")).longValue());
        response.setAuctionId(((Number) row.get("auction_id")).longValue());
        response.setIdeaId(((Number) row.get("idea_id")).longValue());
        response.setIdeaTitle((String) row.get("idea_title"));

        double amount = ((Number) row.get("bid_amount")).doubleValue();
        response.setAmount(amount);

        Number highest = (Number) row.get("highest_bid");
        response.setHighestBid(highest != null ? highest.doubleValue() : amount);
        response.setLeading(highest != null && Double.compare(highest.doubleValue(), amount) == 0);

        response.setAuctionStatus((String) row.get("auction_status"));
        response.setEndTime(toLocalDateTime(row.get("end_time")));
        response.setPlacedAt(toLocalDateTime(row.get("created_at")));
        response.setWon(toBoolean(row.get("won")));
        return response;
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
            throw new UnauthorizedException("Authentication required");
        }

        User user = userDao.findByEmail(auth.getName());
        if (user == null) {
            throw new UnauthorizedException("Authenticated user no longer exists");
        }
        if (!"INVESTOR".equalsIgnoreCase(user.getRole())) {
            throw new ForbiddenException("Only investors can place bids");
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

    /** MySQL surfaces boolean expressions as an integer, so accept either shape. */
    private boolean toBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        return false;
    }
}
