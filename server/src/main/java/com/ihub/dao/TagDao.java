package com.ihub.dao;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Repository
public class TagDao {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public TagDao(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<String> findTagNamesByIdeaId(Long ideaId) {
        String sql = """
            SELECT t.name
            FROM tags t
            INNER JOIN idea_tags it ON it.tag_id = t.id
            WHERE it.idea_id = :ideaId
            ORDER BY t.name
        """;
        return jdbcTemplate.queryForList(sql, Map.of("ideaId", ideaId), String.class);
    }

    public void replaceTagsForIdea(Long ideaId, List<String> tagNames) {
        jdbcTemplate.update("DELETE FROM idea_tags WHERE idea_id = :ideaId", Map.of("ideaId", ideaId));

        if (tagNames == null || tagNames.isEmpty()) {
            return;
        }

        for (String rawName : tagNames) {
            if (rawName == null || rawName.isBlank()) {
                continue;
            }
            String name = rawName.trim().toLowerCase();
            Long tagId = findOrCreateTagId(name);
            jdbcTemplate.update(
                    "INSERT IGNORE INTO idea_tags (idea_id, tag_id) VALUES (:ideaId, :tagId)",
                    Map.of("ideaId", ideaId, "tagId", tagId)
            );
        }
    }

    private Long findOrCreateTagId(String name) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT id FROM tags WHERE name = :name",
                    Map.of("name", name),
                    Long.class
            );
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            String sql = "INSERT INTO tags (name, created_at) VALUES (:name, NOW())";
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(sql, new MapSqlParameterSource("name", name), keyHolder, new String[]{"id"});
            return keyHolder.getKey().longValue();
        }
    }

    public List<String> findTagNamesByIdeaIds(List<Long> ideaIds) {
        if (ideaIds == null || ideaIds.isEmpty()) {
            return Collections.emptyList();
        }
        String sql = """
            SELECT t.name
            FROM tags t
            INNER JOIN idea_tags it ON it.tag_id = t.id
            WHERE it.idea_id IN (:ideaIds)
            ORDER BY t.name
        """;
        return jdbcTemplate.queryForList(sql, Map.of("ideaIds", ideaIds), String.class);
    }
}
