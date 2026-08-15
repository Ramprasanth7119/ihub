package com.ihub.dao;

import com.ihub.model.AuctionBidContext;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public class BidDao {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public BidDao(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Takes an exclusive row lock on the auction so concurrent bids on the same
     * auction are serialised for the duration of the calling transaction. Only the
     * auction row is locked ({@code FOR UPDATE OF a}) — locking the joined idea row
     * as well would needlessly block unrelated creator operations.
     *
     * @return the locked snapshot, or {@code null} when the auction does not exist
     */
    public AuctionBidContext lockAuctionForBid(Long auctionId) {
        // The window comparison is evaluated by MySQL rather than in Java. The
        // scheduler's start/close sweeps already use NOW(), so deciding "is this
        // auction open?" against the same clock keeps the two consistent even when
        // the JVM and the database server run in different time zones.
        String sql = """
            SELECT a.status, a.min_bid_increment, a.start_time, a.end_time,
                   (NOW() < a.start_time) AS before_start,
                   (NOW() >= a.end_time)  AS after_end,
                   i.base_price, i.creator_id AS idea_creator_id
            FROM auctions a
            INNER JOIN ideas i ON i.id = a.idea_id
            WHERE a.id = :id
            FOR UPDATE OF a
        """;

        try {
            return jdbcTemplate.queryForObject(sql, Map.of("id", auctionId), (rs, rowNum) -> {
                AuctionBidContext ctx = new AuctionBidContext();
                ctx.setStatus(rs.getString("status"));
                Double minIncrement = rs.getObject("min_bid_increment", Double.class);
                ctx.setMinBidIncrement(minIncrement != null ? minIncrement : 100.0);
                ctx.setBasePrice(rs.getDouble("base_price"));
                ctx.setIdeaCreatorId(rs.getLong("idea_creator_id"));
                ctx.setStartTime(toLocalDateTime(rs.getTimestamp("start_time")));
                ctx.setEndTime(toLocalDateTime(rs.getTimestamp("end_time")));
                ctx.setBeforeStart(rs.getBoolean("before_start"));
                ctx.setAfterEnd(rs.getBoolean("after_end"));
                return ctx;
            });
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    private static LocalDateTime toLocalDateTime(java.sql.Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }

    public Double getHighestBid(Long auctionId) {
        return jdbcTemplate.queryForObject(
                "SELECT MAX(bid_amount) FROM bids WHERE auction_id = :id",
                Map.of("id", auctionId),
                Double.class
        );
    }

    public Long saveBid(Long auctionId, Long investorId, Double amount) {
        String sql = """
            INSERT INTO bids (auction_id, investor_id, bid_amount, created_at)
            VALUES (:auctionId, :investorId, :amount, NOW())
        """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("auctionId", auctionId)
                .addValue("investorId", investorId)
                .addValue("amount", amount);

        org.springframework.jdbc.support.GeneratedKeyHolder keyHolder =
                new org.springframework.jdbc.support.GeneratedKeyHolder();
        jdbcTemplate.update(sql, params, keyHolder, new String[]{"id"});
        return keyHolder.getKey().longValue();
    }

    public List<Map<String, Object>> findBidHistory(Long auctionId, int limit, int offset) {
        String sql = """
            SELECT b.id AS bid_id, b.investor_id, u.name AS investor_name,
                   b.bid_amount, b.created_at
            FROM bids b
            INNER JOIN users u ON u.id = b.investor_id
            WHERE b.auction_id = :auctionId
            ORDER BY b.created_at DESC, b.id DESC
            LIMIT :limit OFFSET :offset
        """;
        return jdbcTemplate.queryForList(sql, Map.of(
                "auctionId", auctionId, "limit", limit, "offset", offset));
    }

    public long countBidsForAuction(Long auctionId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM bids WHERE auction_id = :auctionId",
                Map.of("auctionId", auctionId),
                Long.class
        );
        return count != null ? count : 0L;
    }

    /** Number of distinct investors who have bid — drives the "N bidders" figure in the live auction UI. */
    public int countDistinctBidders(Long auctionId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT investor_id) FROM bids WHERE auction_id = :auctionId",
                Map.of("auctionId", auctionId),
                Integer.class
        );
        return count != null ? count : 0;
    }

    /**
     * An investor's own bidding history across every auction, annotated with the idea
     * title, auction status and whether the bid is currently the leading one — so the
     * client can render "my bids" without a request per auction.
     */
    public List<Map<String, Object>> findBidsByInvestor(Long investorId, int limit, int offset) {
        String sql = """
            SELECT b.id AS bid_id, b.auction_id, b.bid_amount, b.created_at,
                   i.id AS idea_id, i.title AS idea_title,
                   a.status AS auction_status, a.end_time,
                   top.max_amount AS highest_bid,
                   (aw.winner_id IS NOT NULL AND aw.winner_id = b.investor_id) AS won
            FROM bids b
            INNER JOIN auctions a ON a.id = b.auction_id
            INNER JOIN ideas i ON i.id = a.idea_id
            LEFT JOIN auction_winners aw ON aw.auction_id = a.id
            LEFT JOIN (
                SELECT auction_id, MAX(bid_amount) AS max_amount
                FROM bids
                GROUP BY auction_id
            ) top ON top.auction_id = b.auction_id
            WHERE b.investor_id = :investorId
            ORDER BY b.created_at DESC, b.id DESC
            LIMIT :limit OFFSET :offset
        """;
        return jdbcTemplate.queryForList(sql, Map.of(
                "investorId", investorId, "limit", limit, "offset", offset));
    }

    public long countBidsByInvestor(Long investorId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM bids WHERE investor_id = :investorId",
                Map.of("investorId", investorId),
                Long.class
        );
        return count != null ? count : 0L;
    }

    public Map<String, Object> findHighestBid(Long auctionId) {
        try {
            String sql = """
                SELECT b.id AS bid_id, b.investor_id, u.name AS investor_name,
                       b.bid_amount, b.created_at
                FROM bids b
                INNER JOIN users u ON u.id = b.investor_id
                WHERE b.auction_id = :auctionId
                ORDER BY b.bid_amount DESC, b.created_at ASC
                LIMIT 1
            """;
            return jdbcTemplate.queryForMap(sql, Map.of("auctionId", auctionId));
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public List<Map<String, Object>> findLeaderboard(Long auctionId) {
        String sql = """
            SELECT investor_id, investor_name, bid_amount, latest_bid_at
            FROM (
                SELECT b.investor_id, u.name AS investor_name, b.bid_amount, b.created_at AS latest_bid_at,
                       ROW_NUMBER() OVER (
                           PARTITION BY b.investor_id
                           ORDER BY b.bid_amount DESC, b.created_at ASC
                       ) AS rn
                FROM bids b
                INNER JOIN users u ON u.id = b.investor_id
                WHERE b.auction_id = :auctionId
            ) ranked
            WHERE rn = 1
            ORDER BY bid_amount DESC, latest_bid_at ASC
        """;
        return jdbcTemplate.queryForList(sql, Map.of("auctionId", auctionId));
    }

    public Long findLeaderInvestorId(Long auctionId) {
        try {
            return jdbcTemplate.queryForObject("""
                SELECT b.investor_id
                FROM bids b
                WHERE b.auction_id = :auctionId
                ORDER BY b.bid_amount DESC, b.created_at ASC
                LIMIT 1
            """, Map.of("auctionId", auctionId), Long.class);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public List<Long> findDistinctBidderIds(Long auctionId) {
        return jdbcTemplate.queryForList("""
            SELECT DISTINCT investor_id FROM bids WHERE auction_id = :auctionId
        """, Map.of("auctionId", auctionId), Long.class);
    }

    public String findIdeaTitleByAuction(Long auctionId) {
        return jdbcTemplate.queryForObject("""
            SELECT i.title
            FROM ideas i
            INNER JOIN auctions a ON a.idea_id = i.id
            WHERE a.id = :auctionId
        """, Map.of("auctionId", auctionId), String.class);
    }
}
