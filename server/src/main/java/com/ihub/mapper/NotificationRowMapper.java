package com.ihub.mapper;

import com.ihub.model.Notification;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class NotificationRowMapper implements RowMapper<Notification> {

    @Override
    public Notification mapRow(ResultSet rs, int rowNum) throws SQLException {
        Notification notification = new Notification();
        notification.setId(rs.getLong("id"));
        notification.setUserId(rs.getLong("user_id"));
        notification.setType(rs.getString("type"));
        notification.setTitle(rs.getString("title"));
        notification.setMessage(rs.getString("message"));
        notification.setReferenceType(rs.getString("reference_type"));
        Long refId = rs.getObject("reference_id", Long.class);
        notification.setReferenceId(refId);
        notification.setReadFlag(rs.getBoolean("read_flag"));
        notification.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return notification;
    }
}
