package com.ihub.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.ihub.mapper.AuctionRowMapper;
import com.ihub.model.Auction;

import java.util.List;
import java.util.Map;

/**
 * DAO for auction lifecycle operations
 */
@Repository
@RequiredArgsConstructor
public class AuctionLifecycleDao {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final AuctionRowMapper auctionRowMapper;


	public AuctionLifecycleDao(NamedParameterJdbcTemplate jdbcTemplate, AuctionRowMapper auctionRowMapper) {
		super();
		this.jdbcTemplate = jdbcTemplate;
		this.auctionRowMapper = auctionRowMapper;
	}

	/**
     * Activate auctions
     */
    public void activateAuctions() {

        String sql = """
            UPDATE auctions
            SET status = 'ACTIVE'
            WHERE status = 'SCHEDULED'
        """;

        jdbcTemplate.update(sql, Map.of());
    }

    /**
     * Close auctions
     */
    public void closeAuctions() {

        String sql = """
            UPDATE auctions
            SET status = 'CLOSED'
            WHERE status = 'ACTIVE'
        """;

        jdbcTemplate.update(sql, Map.of());
    }

    /**
     * Get highest bidder per auction
     */
    public List<Map<String, Object>> getWinners() {

        String sql = """
            SELECT b.auction_id, b.investor_id, b.bid_amount
            FROM bids b
            INNER JOIN (
                SELECT auction_id, MAX(bid_amount) AS max_bid
                FROM bids
                GROUP BY auction_id
            ) m
            ON b.auction_id = m.auction_id
            AND b.bid_amount = m.max_bid
        """;

        return jdbcTemplate.queryForList(sql, Map.of());
    }

    /**
     * Save winner
     */
    public void saveWinner(Long auctionId, Long investorId, Double amount) {

        String sql = """
            INSERT INTO auction_winners (auction_id, winner_id, winning_bid)
            VALUES (:auctionId, :winnerId, :amount)
        """;

        jdbcTemplate.update(sql, Map.of(
                "auctionId", auctionId,
                "winnerId", investorId,
                "amount", amount
        ));
    }
    
    public List<Auction> getScheduledAuctions() {
        String sql = "SELECT id, idea_id FROM auctions WHERE status = 'SCHEDULED'";
        return jdbcTemplate.query(sql, auctionRowMapper);
    }

    public List<Auction> getActiveAuctions() {
        String sql = "SELECT id, idea_id FROM auctions WHERE status = 'ACTIVE'";
        return jdbcTemplate.query(sql, auctionRowMapper);
    }
}