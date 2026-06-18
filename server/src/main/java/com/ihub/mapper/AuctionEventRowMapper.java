package com.ihub.mapper;

import com.ihub.model.AuctionEvent;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class AuctionEventRowMapper implements RowMapper<AuctionEvent> {

    @Override
    public AuctionEvent mapRow(ResultSet rs, int rowNum) throws SQLException {
        AuctionEvent event = new AuctionEvent();
        event.setId(rs.getLong("id"));
        event.setAuctionId(rs.getLong("auction_id"));
        event.setEventType(rs.getString("event_type"));
        event.setDetails(rs.getString("details"));
        event.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return event;
    }
}
