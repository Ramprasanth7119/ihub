package com.ihub.mapper;

import com.ihub.model.Bid;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class BidRowMapper implements RowMapper<Bid> {

    @Override
    public Bid mapRow(ResultSet rs, int rowNum) throws SQLException {

        Bid bid = new Bid();
        bid.setId(rs.getLong("id"));
        bid.setAuctionId(rs.getLong("auction_id"));
        bid.setInvestorId(rs.getLong("investor_id"));
        bid.setBidAmount(rs.getDouble("bid_amount"));
        bid.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());

        return bid;
    }
}