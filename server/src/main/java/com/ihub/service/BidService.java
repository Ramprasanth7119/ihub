package com.ihub.service;

import com.ihub.dao.BidDao;
import com.ihub.dao.UserDao;
import com.ihub.dto.BidRequest;
import com.ihub.dto.BidResponse;
import com.ihub.exception.CustomException;
import com.ihub.model.User;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for bidding
 * Ensures:
 * - Only INVESTOR can place bid
 * - No concurrent bid conflicts
 * - Real-time leaderboard updates
 */
@Service
@RequiredArgsConstructor
public class BidService {

    private final BidDao bidDao;
    private final UserDao userDao; // ✅ inject properly
    private final BidBroadcastService broadcastService;

    public BidService(BidDao bidDao, UserDao userDao, BidBroadcastService broadcastService) {
		super();
		this.bidDao = bidDao;
		this.userDao = userDao;
		this.broadcastService = broadcastService;
	}

	/**
     * Place bid with full validation and security
     */
    @Transactional
    public BidResponse placeBid(BidRequest request) {

        // 🔐 Step 1: Get logged-in user from JWT
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new CustomException("User not authenticated");
        }

        String email = auth.getName();

        // 👤 Step 2: Fetch user from DB
        User user = userDao.findByEmail(email);

        if (user == null) {
            throw new CustomException("User not found");
        }

        // 🚫 Step 3: Role validation
        if (!"INVESTOR".equalsIgnoreCase(user.getRole())) {
            throw new CustomException("Only investors can place bids");
        }

        Long investorId = user.getId();

        // 🔒 Step 4: Lock auction row (prevents race condition)
        String status = bidDao.lockAuction(request.getAuctionId());

        if (!"ACTIVE".equalsIgnoreCase(status)) {
            throw new CustomException("Auction is not active");
        }

        // 📈 Step 5: Validate bid amount
        Double current = bidDao.getHighestBid(request.getAuctionId());

        if (current != null && request.getAmount() <= current) {
            throw new CustomException("Bid must be higher than current highest");
        }

        // 💾 Step 6: Save bid (use investorId from JWT, NOT request)
        bidDao.saveBid(
                request.getAuctionId(),
                investorId,
                request.getAmount()
        );

        // 📊 Step 7: Get updated leaderboard
        List<Map<String, Object>> leaderboard =
                bidDao.getLeaderboard(request.getAuctionId());

        System.out.println("Leaderboard: " + leaderboard);

        // 🔌 Step 8: Broadcast leaderboard via WebSocket
        broadcastService.broadcastLeaderboard(
                request.getAuctionId(),
                leaderboard
        );

        // 📤 Step 9: Response
        return new BidResponse(
                "Bid placed successfully",
                request.getAmount()
        );
    }
}