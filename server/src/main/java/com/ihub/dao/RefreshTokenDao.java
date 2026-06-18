package com.ihub.dao;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Map;

@Repository
public class RefreshTokenDao {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public RefreshTokenDao(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(Long userId, String token, LocalDateTime expiresAt) {
        String sql = """
            INSERT INTO refresh_tokens (user_id, token, expires_at, revoked, created_at)
            VALUES (:userId, :token, :expiresAt, false, NOW())
        """;

        jdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("token", token)
                .addValue("expiresAt", expiresAt));
    }

    public Map<String, Object> findValidToken(String token) {
        try {
            String sql = """
                SELECT rt.id, rt.user_id, rt.token, rt.expires_at, u.email, u.role
                FROM refresh_tokens rt
                INNER JOIN users u ON u.id = rt.user_id
                WHERE rt.token = :token
                  AND rt.revoked = false
                  AND rt.expires_at > NOW()
                  AND u.active = true
            """;
            return jdbcTemplate.queryForMap(sql, Map.of("token", token));
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return null;
        }
    }

    public void revoke(String token) {
        String sql = "UPDATE refresh_tokens SET revoked = true WHERE token = :token";
        jdbcTemplate.update(sql, Map.of("token", token));
    }

    public void revokeAllForUser(Long userId) {
        String sql = "UPDATE refresh_tokens SET revoked = true WHERE user_id = :userId";
        jdbcTemplate.update(sql, Map.of("userId", userId));
    }
}
