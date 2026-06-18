package com.ihub.mapper;

import com.ihub.model.Auction;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Maps DB result to Auction object
 */
@Component
public class AuctionRowMapper implements RowMapper<Auction> {

    @Override
    public Auction mapRow(ResultSet rs, int rowNum) throws SQLException {

        Auction auction = new Auction();
        auction.setId(rs.getLong("id"));
        auction.setIdeaId(rs.getLong("idea_id"));

        var startTime = rs.getTimestamp("start_time");
        if (startTime != null) {
            auction.setStartTime(startTime.toLocalDateTime());
        }
        var endTime = rs.getTimestamp("end_time");
        if (endTime != null) {
            auction.setEndTime(endTime.toLocalDateTime());
        }

        Double minIncrement = rs.getObject("min_bid_increment", Double.class);
        if (minIncrement != null) {
            auction.setMinBidIncrement(minIncrement);
        }

        auction.setStatus(rs.getString("status"));

        return auction;
    }
}