package com.ihub.dao;

import com.ihub.dto.IdeaRequest;
import com.ihub.mapper.IdeaRowMapper;
import com.ihub.model.Idea;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * DAO layer for Idea operations
 */
@Repository
@RequiredArgsConstructor
public class IdeaDao {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final IdeaRowMapper ideaRowMapper;
    
    

    public IdeaDao(NamedParameterJdbcTemplate jdbcTemplate, IdeaRowMapper ideaRowMapper) {
		super();
		this.jdbcTemplate = jdbcTemplate;
		this.ideaRowMapper = ideaRowMapper;
	}

	/**
     * Create new idea
     */
    public Long createIdea(IdeaRequest request) {

        String sql = """
            INSERT INTO ideas (creator_id, title, description, category, base_price, status, created_at)
            VALUES (:creatorId, :title, :description, :category, :basePrice, 'OPEN', NOW())
        """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("creatorId", request.getCreatorId())
                .addValue("title", request.getTitle())
                .addValue("description", request.getDescription())
                .addValue("category", request.getCategory())
                .addValue("basePrice", request.getBasePrice());

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(sql, params, keyHolder, new String[]{"id"});

        return keyHolder.getKey().longValue();
    }

    /**
     * Fetch idea by ID
     */
    public Idea getIdeaById(Long id) {

        String sql = "SELECT * FROM ideas WHERE id = :id";

        return jdbcTemplate.queryForObject(
                sql,
                Map.of("id", id),
                ideaRowMapper
        );
    }

    /**
     * Fetch all ideas
     */
    public List<Idea> getAllIdeas() {

        String sql = "SELECT * FROM ideas ORDER BY created_at DESC";

        return jdbcTemplate.query(sql, ideaRowMapper);
    }

    /**
     * Check if creator exists
     */
    public boolean existsUser(Long userId) {

        String sql = "SELECT COUNT(*) FROM users WHERE id = :id";

        Integer count = jdbcTemplate.queryForObject(
                sql,
                Map.of("id", userId),
                Integer.class
        );

        return count != null && count > 0;
    }
}