package com.ihub.dao;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Key/value store for platform settings.
 *
 * <p>A narrow key/value table keeps adding a setting a code-only change: no schema
 * migration is needed per field, and unknown keys left behind by an older release
 * are simply ignored when the settings object is assembled.</p>
 */
@Repository
public class PlatformSettingsDao {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public PlatformSettingsDao(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, String> findAll() {
        Map<String, String> settings = new LinkedHashMap<>();
        jdbcTemplate.query(
                "SELECT setting_key, setting_value FROM platform_settings",
                rs -> {
                    settings.put(rs.getString("setting_key"), rs.getString("setting_value"));
                });
        return settings;
    }

    /** Upserts every supplied key in one batch. */
    public void saveAll(Map<String, String> settings, Long updatedBy) {
        if (settings.isEmpty()) {
            return;
        }

        String sql = """
            INSERT INTO platform_settings (setting_key, setting_value, updated_by, updated_at)
            VALUES (:key, :value, :updatedBy, NOW())
            ON DUPLICATE KEY UPDATE
                setting_value = VALUES(setting_value),
                updated_by = VALUES(updated_by),
                updated_at = NOW()
        """;

        MapSqlParameterSource[] batch = settings.entrySet().stream()
                .map(entry -> new MapSqlParameterSource()
                        .addValue("key", entry.getKey())
                        .addValue("value", entry.getValue())
                        .addValue("updatedBy", updatedBy))
                .toArray(MapSqlParameterSource[]::new);

        jdbcTemplate.batchUpdate(sql, batch);
    }
}
