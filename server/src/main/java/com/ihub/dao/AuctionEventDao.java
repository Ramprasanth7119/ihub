package com.ihub.dao;

import com.ihub.mapper.AuctionEventRowMapper;
import com.ihub.model.AuctionEvent;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class AuctionEventDao {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final AuctionEventRowMapper eventRowMapper;

    public AuctionEventDao(NamedParameterJdbcTemplate jdbcTemplate, AuctionEventRowMapper eventRowMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.eventRowMapper = eventRowMapper;
    }

    public void recordEvent(Long auctionId, String eventType, String details) {
        String sql = """
            INSERT INTO auction_events (auction_id, event_type, details, created_at)
            VALUES (:auctionId, :eventType, :details, NOW())
        """;
        jdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("auctionId", auctionId)
                .addValue("eventType", eventType)
                .addValue("details", details));
    }

    public List<AuctionEvent> findByAuctionId(Long auctionId) {
        return jdbcTemplate.query(
                "SELECT * FROM auction_events WHERE auction_id = :auctionId ORDER BY created_at ASC",
                Map.of("auctionId", auctionId),
                eventRowMapper
        );
    }
}
