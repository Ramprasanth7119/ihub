package com.ihub.dao;

import com.ihub.dto.IdeaRequest;
import com.ihub.dto.IdeaUpdateRequest;
import com.ihub.mapper.IdeaRowMapper;
import com.ihub.model.Idea;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class IdeaDao {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final IdeaRowMapper ideaRowMapper;

    public IdeaDao(NamedParameterJdbcTemplate jdbcTemplate, IdeaRowMapper ideaRowMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.ideaRowMapper = ideaRowMapper;
    }

    public Long createIdea(IdeaRequest request, Long creatorId) {
        Double maxBudget = request.getMaxBudget() != null ? request.getMaxBudget() : request.getBasePrice();

        String sql = """
            INSERT INTO ideas (creator_id, title, description, category, base_price, max_budget, status, created_at)
            VALUES (:creatorId, :title, :description, :category, :basePrice, :maxBudget, 'DRAFT', NOW())
        """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("creatorId", creatorId)
                .addValue("title", request.getTitle())
                .addValue("description", request.getDescription())
                .addValue("category", request.getCategory().toLowerCase())
                .addValue("basePrice", request.getBasePrice())
                .addValue("maxBudget", maxBudget);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, params, keyHolder, new String[]{"id"});
        return keyHolder.getKey().longValue();
    }

    public void updateIdea(Long id, IdeaUpdateRequest request) {
        String sql = """
            UPDATE ideas
            SET title = COALESCE(:title, title),
                description = COALESCE(:description, description),
                category = COALESCE(:category, category),
                base_price = COALESCE(:basePrice, base_price),
                max_budget = COALESCE(:maxBudget, max_budget)
            WHERE id = :id AND status = 'DRAFT'
        """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("title", request.getTitle())
                .addValue("description", request.getDescription())
                .addValue("category", request.getCategory() != null ? request.getCategory().toLowerCase() : null)
                .addValue("basePrice", request.getBasePrice())
                .addValue("maxBudget", request.getMaxBudget());

        int updated = jdbcTemplate.update(sql, params);
        if (updated == 0) {
            throw new org.springframework.dao.EmptyResultDataAccessException(1);
        }
    }

    public void publishIdea(Long id) {
        int updated = jdbcTemplate.update(
                "UPDATE ideas SET status = 'PUBLISHED' WHERE id = :id AND status = 'DRAFT'",
                Map.of("id", id)
        );
        if (updated == 0) {
            throw new org.springframework.dao.EmptyResultDataAccessException(1);
        }
    }

    public void archiveIdea(Long id) {
        int updated = jdbcTemplate.update(
                "UPDATE ideas SET status = 'ARCHIVED' WHERE id = :id AND status != 'ARCHIVED'",
                Map.of("id", id)
        );
        if (updated == 0) {
            throw new org.springframework.dao.EmptyResultDataAccessException(1);
        }
    }

    public Idea getIdeaById(Long id) {
        return jdbcTemplate.queryForObject(
                "SELECT * FROM ideas WHERE id = :id",
                Map.of("id", id),
                ideaRowMapper
        );
    }

    public List<Idea> findIdeas(String status, String category, Double minBudget, Double maxBudget, Long creatorId) {
        StringBuilder sql = new StringBuilder("SELECT * FROM ideas WHERE status != 'ARCHIVED'");
        MapSqlParameterSource params = new MapSqlParameterSource();

        if (status != null && !status.isBlank()) {
            sql.append(" AND status = :status");
            params.addValue("status", status.toUpperCase());
        }
        if (category != null && !category.isBlank()) {
            sql.append(" AND category = :category");
            params.addValue("category", category.toLowerCase());
        }
        if (minBudget != null) {
            sql.append(" AND max_budget >= :minBudget");
            params.addValue("minBudget", minBudget);
        }
        if (maxBudget != null) {
            sql.append(" AND base_price <= :maxBudget");
            params.addValue("maxBudget", maxBudget);
        }
        if (creatorId != null) {
            sql.append(" AND creator_id = :creatorId");
            params.addValue("creatorId", creatorId);
        }

        sql.append(" ORDER BY created_at DESC");
        return jdbcTemplate.query(sql.toString(), params, ideaRowMapper);
    }

    public boolean existsUser(Long userId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE id = :id",
                Map.of("id", userId),
                Integer.class
        );
        return count != null && count > 0;
    }

    public Long getCreatorId(Long ideaId) {
        return jdbcTemplate.queryForObject(
                "SELECT creator_id FROM ideas WHERE id = :id",
                Map.of("id", ideaId),
                Long.class
        );
    }
}
