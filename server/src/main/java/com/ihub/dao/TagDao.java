package com.ihub.dao;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
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

    /**
     * Loads the tags for many ideas in a single round trip, keyed by idea id.
     *
     * <p>Mapping a list of ideas used to call {@link #findTagNamesByIdeaId} once per
     * idea, which is an N+1 query against {@code idea_tags}. Callers that render a
     * collection should use this instead; ideas with no tags are simply absent from
     * the map.</p>
     */
    public Map<Long, List<String>> findTagNamesGroupedByIdeaIds(List<Long> ideaIds) {
        if (ideaIds == null || ideaIds.isEmpty()) {
            return Collections.emptyMap();
        }

        String sql = """
            SELECT it.idea_id AS idea_id, t.name AS name
            FROM tags t
            INNER JOIN idea_tags it ON it.tag_id = t.id
            WHERE it.idea_id IN (:ideaIds)
            ORDER BY it.idea_id, t.name
        """;

        Map<Long, List<String>> grouped = new LinkedHashMap<>();
        jdbcTemplate.query(sql, Map.of("ideaIds", ideaIds), rs -> {
            Long ideaId = rs.getLong("idea_id");
            grouped.computeIfAbsent(ideaId, key -> new ArrayList<>()).add(rs.getString("name"));
        });
        return grouped;
    }
}
