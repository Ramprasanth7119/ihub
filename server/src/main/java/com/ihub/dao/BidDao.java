package com.ihub.dao;

import com.ihub.dto.BidRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * DAO for bidding operations
 */
@Repository
public class BidDao {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    
    public BidDao(NamedParameterJdbcTemplate jdbcTemplate) {
		super();
		this.jdbcTemplate = jdbcTemplate;
	}

	/**
     * Lock auction row to prevent concurrent updates
     */
    public String lockAuction(Long auctionId) {

        String sql = "SELECT status FROM auctions WHERE id = :id FOR UPDATE";

        List<String> result = jdbcTemplate.query(
                sql,
                Map.of("id", auctionId),
                (rs, rowNum) -> rs.getString("status")
        );

        if (result.isEmpty()) {
            throw new RuntimeException("Auction not found with id: " + auctionId);
        }

        return result.get(0);
    }

    /**
     * Get current highest bid
     */
    public Double getHighestBid(Long auctionId) {

        String sql = "SELECT MAX(bid_amount) FROM bids WHERE auction_id = :id";

        return jdbcTemplate.queryForObject(
                sql,
                Map.of("id", auctionId),
                Double.class
        );
    }

    /**
     * Save new bid
     */
    public void saveBid(Long auctionId, Long investorId, Double amount) {

        String sql = """
            INSERT INTO bids (auction_id, investor_id, bid_amount, created_at)
            VALUES (:auctionId, :investorId, :amount, NOW())
        """;

        Map<String, Object> params = Map.of(
                "auctionId", auctionId,
                "investorId", investorId,
                "amount", amount
        );

        jdbcTemplate.update(sql, params);
    } 
    
    public Integer getRank(Long auctionId, Double amount) {

        String sql = """
            SELECT COUNT(*) + 1
            FROM bids
            WHERE auction_id = :auctionId
            AND bid_amount > :amount
        """;

        return jdbcTemplate.queryForObject(
                sql,
                Map.of("auctionId", auctionId, "amount", amount),
                Integer.class
        );
    }
    
    public List<Map<String, Object>> getLeaderboard(Long auctionId) {

        String sql = """
            SELECT b1.investor_id, b1.bid_amount,
            (
                SELECT COUNT(*) + 1
                FROM bids b2
                WHERE b2.auction_id = b1.auction_id
                AND b2.bid_amount > b1.bid_amount
            ) AS bid_rank
            FROM bids b1
            WHERE b1.auction_id = :auctionId
            ORDER BY b1.bid_amount DESC
        """;

        return jdbcTemplate.queryForList(
                sql,
                Map.of("auctionId", auctionId)
        );
    }
}