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

    public AuctionBidContext lockAuctionForBid(Long auctionId) {
        String sql = """
            SELECT a.status, a.min_bid_increment, i.base_price, i.creator_id AS idea_creator_id
            FROM auctions a
            INNER JOIN ideas i ON i.id = a.idea_id
            WHERE a.id = :id
            FOR UPDATE
        """;

        try {
            return jdbcTemplate.queryForObject(sql, Map.of("id", auctionId), (rs, rowNum) -> {
                AuctionBidContext ctx = new AuctionBidContext();
                ctx.setStatus(rs.getString("status"));
                Double minIncrement = rs.getObject("min_bid_increment", Double.class);
                ctx.setMinBidIncrement(minIncrement != null ? minIncrement : 100.0);
                ctx.setBasePrice(rs.getDouble("base_price"));
                ctx.setIdeaCreatorId(rs.getLong("idea_creator_id"));
                return ctx;
            });
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
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

    public List<Map<String, Object>> findBidHistory(Long auctionId) {
        String sql = """
            SELECT b.id AS bid_id, b.investor_id, u.name AS investor_name,
                   b.bid_amount, b.created_at
            FROM bids b
            INNER JOIN users u ON u.id = b.investor_id
            WHERE b.auction_id = :auctionId
            ORDER BY b.created_at DESC
        """;
        return jdbcTemplate.queryForList(sql, Map.of("auctionId", auctionId));
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
