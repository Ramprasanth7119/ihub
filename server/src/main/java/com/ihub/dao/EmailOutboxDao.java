package com.ihub.dao;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class EmailOutboxDao {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public EmailOutboxDao(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long enqueue(String recipientEmail, String subject, String body) {
        String sql = """
            INSERT INTO email_outbox (recipient_email, subject, body, status, attempts, created_at)
            VALUES (:email, :subject, :body, 'PENDING', 0, NOW())
        """;

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("email", recipientEmail)
                .addValue("subject", subject)
                .addValue("body", body), keyHolder, new String[]{"id"});
        return keyHolder.getKey().longValue();
    }

    public List<Map<String, Object>> findPending(int batchSize, int maxAttempts) {
        String sql = """
            SELECT id, recipient_email, subject, body, attempts
            FROM email_outbox
            WHERE status = 'PENDING' AND attempts < :maxAttempts
            ORDER BY created_at ASC
            LIMIT :batchSize
        """;
        return jdbcTemplate.queryForList(sql, Map.of(
                "batchSize", batchSize,
                "maxAttempts", maxAttempts
        ));
    }

    public void markSent(Long id) {
        jdbcTemplate.update(
                "UPDATE email_outbox SET status = 'SENT', sent_at = NOW() WHERE id = :id",
                Map.of("id", id)
        );
    }

    public void markFailed(Long id, String error, int maxAttempts) {
        jdbcTemplate.update("""
            UPDATE email_outbox
            SET attempts = attempts + 1, last_error = :error,
                status = CASE WHEN attempts + 1 >= :maxAttempts THEN 'FAILED' ELSE 'PENDING' END
            WHERE id = :id
        """, Map.of("id", id, "error", truncate(error, 500), "maxAttempts", maxAttempts));
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
