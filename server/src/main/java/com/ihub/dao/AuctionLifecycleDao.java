package com.ihub.dao;

import com.ihub.mapper.AuctionRowMapper;
import com.ihub.model.Auction;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Repository
public class AuctionLifecycleDao {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final AuctionRowMapper auctionRowMapper;

    public AuctionLifecycleDao(NamedParameterJdbcTemplate jdbcTemplate, AuctionRowMapper auctionRowMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.auctionRowMapper = auctionRowMapper;
    }

    public List<Auction> findDueToStart() {
        String sql = """
            SELECT * FROM auctions
            WHERE status = 'SCHEDULED' AND start_time <= NOW()
            ORDER BY start_time ASC
        """;
        return jdbcTemplate.query(sql, auctionRowMapper);
    }

    public List<Auction> findDueToClose() {
        String sql = """
            SELECT * FROM auctions
            WHERE status = 'ACTIVE' AND end_time <= NOW()
            ORDER BY end_time ASC
        """;
        return jdbcTemplate.query(sql, auctionRowMapper);
    }

    /**
     * Active auctions whose end time falls inside the next {@code minutes} window and
     * that have not already been warned.
     *
     * <p>The {@code auction_events} table doubles as the idempotency ledger: an
     * auction with an {@code ENDING_SOON} row is skipped, so repeated scheduler runs
     * (or a restart mid-window) cannot notify the same participants twice.</p>
     */
    public List<Auction> findEndingSoon(int minutes) {
        String sql = """
            SELECT a.* FROM auctions a
            WHERE a.status = 'ACTIVE'
              AND a.end_time > NOW()
              AND a.end_time <= DATE_ADD(NOW(), INTERVAL :minutes MINUTE)
              AND NOT EXISTS (
                  SELECT 1 FROM auction_events e
                  WHERE e.auction_id = a.id AND e.event_type = 'ENDING_SOON'
              )
            ORDER BY a.end_time ASC
        """;
        return jdbcTemplate.query(sql, Map.of("minutes", minutes), auctionRowMapper);
    }

    public int activateById(Long auctionId) {
        return jdbcTemplate.update(
                "UPDATE auctions SET status = 'ACTIVE' WHERE id = :id AND status = 'SCHEDULED'",
                Map.of("id", auctionId)
        );
    }

    /**
     * Activates an auction ahead of its schedule, pulling {@code start_time} forward
     * to now.
     *
     * <p>Bid validation checks the auction window as well as the status, so leaving a
     * future start time in place would activate an auction that still rejected every
     * bid with "has not started yet".</p>
     */
    public int activateNowById(Long auctionId) {
        return jdbcTemplate.update("""
            UPDATE auctions
            SET status = 'ACTIVE',
                start_time = LEAST(start_time, NOW())
            WHERE id = :id AND status = 'SCHEDULED'
        """, Map.of("id", auctionId));
    }

    public int closeById(Long auctionId) {
        return jdbcTemplate.update(
                "UPDATE auctions SET status = 'CLOSED' WHERE id = :id AND status = 'ACTIVE'",
                Map.of("id", auctionId)
        );
    }

    /** Cancels an auction that has not yet reached a terminal state. */
    public int cancelById(Long auctionId) {
        return jdbcTemplate.update(
                """
                    UPDATE auctions SET status = 'CANCELLED'
                    WHERE id = :id AND status NOT IN ('CLOSED', 'CANCELLED')
                """,
                Map.of("id", auctionId)
        );
    }

    /**
     * Highest bid wins; ties broken by earliest bid time.
     */
    public List<Map<String, Object>> findWinnersForAuctions(List<Long> auctionIds) {
        if (auctionIds == null || auctionIds.isEmpty()) {
            return Collections.emptyList();
        }

        String sql = """
            SELECT auction_id, investor_id, bid_amount
            FROM (
                SELECT b.auction_id, b.investor_id, b.bid_amount,
                       ROW_NUMBER() OVER (
                           PARTITION BY b.auction_id
                           ORDER BY b.bid_amount DESC, b.created_at ASC
                       ) AS rn
                FROM bids b
                WHERE b.auction_id IN (:auctionIds)
            ) ranked
            WHERE rn = 1
        """;

        return jdbcTemplate.queryForList(sql, Map.of("auctionIds", auctionIds));
    }

    public void saveWinner(Long auctionId, Long investorId, Double amount) {
        String sql = """
            INSERT INTO auction_winners (auction_id, winner_id, winning_bid, created_at)
            VALUES (:auctionId, :winnerId, :amount, NOW())
            ON DUPLICATE KEY UPDATE winner_id = VALUES(winner_id), winning_bid = VALUES(winning_bid)
        """;
        jdbcTemplate.update(sql, Map.of(
                "auctionId", auctionId,
                "winnerId", investorId,
                "amount", amount
        ));
    }

    public boolean hasWinner(Long auctionId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auction_winners WHERE auction_id = :auctionId",
                Map.of("auctionId", auctionId),
                Integer.class
        );
        return count != null && count > 0;
    }
}
