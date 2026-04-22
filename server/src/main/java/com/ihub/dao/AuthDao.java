package com.ihub.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository
@RequiredArgsConstructor
public class AuthDao {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    
    

    public AuthDao(NamedParameterJdbcTemplate jdbcTemplate) {
		super();
		this.jdbcTemplate = jdbcTemplate;
	}

    public Map<String, Object> getUserByEmail(String email) {
        try {
            String sql = "SELECT id, email, password, role FROM users WHERE email = :email";
            return jdbcTemplate.queryForMap(sql, Map.of("email", email));
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return null; // This allows the Service to throw your CustomException
        }
    }

}