package com.ihub.dao;

import com.ihub.mapper.NotificationRowMapper;
import com.ihub.model.Notification;
import com.ihub.notification.NotificationType;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class NotificationDao {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final NotificationRowMapper rowMapper;

    public NotificationDao(NamedParameterJdbcTemplate jdbcTemplate, NotificationRowMapper rowMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.rowMapper = rowMapper;
    }

    public Long insert(
            Long userId,
            NotificationType type,
            String title,
            String message,
            String referenceType,
            Long referenceId) {

        String sql = """
            INSERT INTO notifications (user_id, type, title, message, reference_type, reference_id, read_flag, created_at)
            VALUES (:userId, :type, :title, :message, :referenceType, :referenceId, false, NOW())
        """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("type", type.name())
                .addValue("title", title)
                .addValue("message", message)
                .addValue("referenceType", referenceType)
                .addValue("referenceId", referenceId);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, params, keyHolder, new String[]{"id"});
        return keyHolder.getKey().longValue();
    }

    public List<Notification> findByUserId(Long userId, boolean unreadOnly, int limit, int offset) {
        StringBuilder sql = new StringBuilder("SELECT * FROM notifications WHERE user_id = :userId");
        if (unreadOnly) {
            sql.append(" AND read_flag = false");
        }
        sql.append(" ORDER BY created_at DESC LIMIT :limit OFFSET :offset");

        return jdbcTemplate.query(sql.toString(), Map.of(
                "userId", userId,
                "limit", limit,
                "offset", offset
        ), rowMapper);
    }

    public long countByUserId(Long userId, boolean unreadOnly) {
        String sql = unreadOnly
                ? "SELECT COUNT(*) FROM notifications WHERE user_id = :userId AND read_flag = false"
                : "SELECT COUNT(*) FROM notifications WHERE user_id = :userId";

        Long count = jdbcTemplate.queryForObject(sql, Map.of("userId", userId), Long.class);
        return count != null ? count : 0;
    }

    public Optional<Notification> findByIdAndUserId(Long id, Long userId) {
        try {
            Notification notification = jdbcTemplate.queryForObject(
                    "SELECT * FROM notifications WHERE id = :id AND user_id = :userId",
                    Map.of("id", id, "userId", userId),
                    rowMapper
            );
            return Optional.ofNullable(notification);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public int markAsRead(Long id, Long userId) {
        return jdbcTemplate.update(
                "UPDATE notifications SET read_flag = true WHERE id = :id AND user_id = :userId",
                Map.of("id", id, "userId", userId)
        );
    }

    public int markAllAsRead(Long userId) {
        return jdbcTemplate.update(
                "UPDATE notifications SET read_flag = true WHERE user_id = :userId AND read_flag = false",
                Map.of("userId", userId)
        );
    }
}
