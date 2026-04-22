package com.ihub.dao;

import com.ihub.dto.AuctionRequest;
import com.ihub.mapper.AuctionRowMapper;
import com.ihub.model.Auction;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * DAO for auction operations
 */
@Repository
@RequiredArgsConstructor
public class AuctionDao {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final AuctionRowMapper auctionRowMapper;
    
    

    public AuctionDao(NamedParameterJdbcTemplate jdbcTemplate, AuctionRowMapper auctionRowMapper) {
		super();
		this.jdbcTemplate = jdbcTemplate;
		this.auctionRowMapper = auctionRowMapper;
	}

	public boolean ideaExists(Long ideaId) {

        String sql = "SELECT COUNT(*) FROM ideas WHERE id = :id";

        Integer count = jdbcTemplate.queryForObject(
                sql,
                Map.of("id", ideaId),
                Integer.class
        );

        return count != null && count > 0;
    }

    public boolean auctionExistsForIdea(Long ideaId) {

        String sql = "SELECT COUNT(*) FROM auctions WHERE idea_id = :ideaId";

        Integer count = jdbcTemplate.queryForObject(
                sql,
                Map.of("ideaId", ideaId),
                Integer.class
        );

        return count != null && count > 0;
    }

    public Long createAuction(AuctionRequest request) {

        String sql = """
            INSERT INTO auctions (idea_id, start_time, end_time, status)
            VALUES (:ideaId, :startTime, :endTime, 'SCHEDULED')
        """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("ideaId", request.getIdeaId())
                .addValue("startTime", request.getStartTime())
                .addValue("endTime", request.getEndTime());

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(sql, params, keyHolder, new String[]{"id"});

        return keyHolder.getKey().longValue();
    }

    public Auction getAuctionById(Long id) {

        String sql = "SELECT * FROM auctions WHERE id = :id";

        return jdbcTemplate.queryForObject(
                sql,
                Map.of("id", id),
                auctionRowMapper
        );
    }

    public List<Auction> getAllAuctions() {

        String sql = "SELECT * FROM auctions ORDER BY created_at DESC";

        return jdbcTemplate.query(sql, auctionRowMapper);
    }
}