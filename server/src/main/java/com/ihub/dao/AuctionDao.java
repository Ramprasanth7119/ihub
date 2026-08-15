package com.ihub.dao;

import com.ihub.dto.AuctionRequest;
import com.ihub.mapper.AuctionRowMapper;
import com.ihub.model.Auction;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class AuctionDao {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final AuctionRowMapper auctionRowMapper;

    public AuctionDao(NamedParameterJdbcTemplate jdbcTemplate, AuctionRowMapper auctionRowMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.auctionRowMapper = auctionRowMapper;
    }

    public boolean ideaExists(Long ideaId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ideas WHERE id = :id",
                Map.of("id", ideaId),
                Integer.class
        );
        return count != null && count > 0;
    }

    public boolean isIdeaPublished(Long ideaId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ideas WHERE id = :id AND status = 'PUBLISHED'",
                Map.of("id", ideaId),
                Integer.class
        );
        return count != null && count > 0;
    }

    public Long getIdeaCreatorId(Long ideaId) {
        return jdbcTemplate.queryForObject(
                "SELECT creator_id FROM ideas WHERE id = :id",
                Map.of("id", ideaId),
                Long.class
        );
    }

    /**
     * True when the idea already has an auction that is scheduled or running.
     *
     * <p>Terminal states are excluded: a closed or cancelled auction must not
     * permanently block the idea from being auctioned again.</p>
     */
    public boolean auctionExistsForIdea(Long ideaId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                    SELECT COUNT(*) FROM auctions
                    WHERE idea_id = :ideaId AND status NOT IN ('CLOSED', 'CANCELLED')
                """,
                Map.of("ideaId", ideaId),
                Integer.class
        );
        return count != null && count > 0;
    }

    public Long createAuction(AuctionRequest request, double defaultMinBidIncrement) {
        double minIncrement = request.getMinBidIncrement() != null
                ? request.getMinBidIncrement()
                : defaultMinBidIncrement;

        String sql = """
            INSERT INTO auctions (idea_id, start_time, end_time, min_bid_increment, status, created_at)
            VALUES (:ideaId, :startTime, :endTime, :minBidIncrement, 'SCHEDULED', NOW())
        """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("ideaId", request.getIdeaId())
                .addValue("startTime", request.getStartTime())
                .addValue("endTime", request.getEndTime())
                .addValue("minBidIncrement", minIncrement);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, params, keyHolder, new String[]{"id"});
        return keyHolder.getKey().longValue();
    }

    public Auction getAuctionById(Long id) {
        return jdbcTemplate.queryForObject(
                "SELECT * FROM auctions WHERE id = :id",
                Map.of("id", id),
                auctionRowMapper
        );
    }

    public List<Auction> findAuctions(String status, int limit, int offset) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String where = buildAuctionFilter(status, params);

        String sql = "SELECT * FROM auctions WHERE " + where
                + " ORDER BY created_at DESC, id DESC LIMIT :limit OFFSET :offset";
        params.addValue("limit", limit);
        params.addValue("offset", offset);

        return jdbcTemplate.query(sql, params, auctionRowMapper);
    }

    public long countAuctions(String status) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM auctions WHERE " + buildAuctionFilter(status, params),
                params, Long.class);
        return count != null ? count : 0L;
    }

    private String buildAuctionFilter(String status, MapSqlParameterSource params) {
        if (status == null || status.isBlank()) {
            return "1=1";
        }
        params.addValue("status", status.toUpperCase());
        return "status = :status";
    }

    /** Loads the idea title and creator alongside the auction, for detail views. */
    public Map<String, Object> findAuctionDetail(Long auctionId) {
        try {
            String sql = """
                SELECT a.id, a.idea_id, a.start_time, a.end_time, a.min_bid_increment,
                       a.status, a.created_at,
                       i.title AS idea_title, i.description AS idea_description,
                       i.category AS idea_category, i.base_price,
                       u.id AS creator_id, u.name AS creator_name
                FROM auctions a
                INNER JOIN ideas i ON i.id = a.idea_id
                INNER JOIN users u ON u.id = i.creator_id
                WHERE a.id = :id
            """;
            return jdbcTemplate.queryForMap(sql, Map.of("id", auctionId));
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public Map<String, Object> findWinnerByAuctionId(Long auctionId) {
        try {
            String sql = """
                SELECT aw.auction_id, aw.winner_id, aw.winning_bid, aw.created_at, u.name AS winner_name
                FROM auction_winners aw
                INNER JOIN users u ON u.id = aw.winner_id
                WHERE aw.auction_id = :auctionId
            """;
            return jdbcTemplate.queryForMap(sql, Map.of("auctionId", auctionId));
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }
}
