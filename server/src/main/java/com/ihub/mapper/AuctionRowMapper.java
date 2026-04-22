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
        auction.setStartTime(rs.getTimestamp("start_time").toLocalDateTime());
        auction.setEndTime(rs.getTimestamp("end_time").toLocalDateTime());
        auction.setStatus(rs.getString("status"));

        return auction;
    }
}