package com.ihub.mapper;

import com.ihub.model.Idea;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Maps SQL result to Idea object
 */
@Component
public class IdeaRowMapper implements RowMapper<Idea> {

    @Override
    public Idea mapRow(ResultSet rs, int rowNum) throws SQLException {

        Idea idea = new Idea();
        idea.setId(rs.getLong("id"));
        idea.setCreatorId(rs.getLong("creator_id"));
        idea.setTitle(rs.getString("title"));
        idea.setDescription(rs.getString("description"));
        idea.setCategory(rs.getString("category"));
        idea.setBasePrice(rs.getDouble("base_price"));
        idea.setStatus(rs.getString("status"));

        return idea;
    }
}