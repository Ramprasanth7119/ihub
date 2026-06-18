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

    public boolean auctionExistsForIdea(Long ideaId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                    SELECT COUNT(*) FROM auctions
                    WHERE idea_id = :ideaId AND status != 'CLOSED'
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

    public List<Auction> findAuctions(String status) {
        StringBuilder sql = new StringBuilder("SELECT * FROM auctions WHERE 1=1");
        MapSqlParameterSource params = new MapSqlParameterSource();

        if (status != null && !status.isBlank()) {
            sql.append(" AND status = :status");
            params.addValue("status", status.toUpperCase());
        }

        sql.append(" ORDER BY created_at DESC");
        return jdbcTemplate.query(sql.toString(), params, auctionRowMapper);
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
