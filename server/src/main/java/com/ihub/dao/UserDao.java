package com.ihub.dao;

import com.ihub.dto.UserRequest;
import com.ihub.mapper.UserRowMapper;
import com.ihub.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * DAO layer for user-related DB operations
 */
@Repository
@RequiredArgsConstructor
public class UserDao {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final UserRowMapper userRowMapper;

    public UserDao(NamedParameterJdbcTemplate jdbcTemplate, UserRowMapper userRowMapper) {
		super();
		this.jdbcTemplate = jdbcTemplate;
		this.userRowMapper = userRowMapper;
	}

	/**
     * Creates a new user and returns generated ID
     */
    public Long createUser(UserRequest request) {

        String sql = """
            INSERT INTO users (name, email, password, role, verified, created_at)
            VALUES (:name, :email, :password, :role, false, NOW())
        """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("name", request.getName())
                .addValue("email", request.getEmail())
                .addValue("password", request.getPassword())
                .addValue("role", request.getRole());

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(sql, params, keyHolder, new String[]{"id"});

        return keyHolder.getKey() != null ? keyHolder.getKey().longValue() : null;
    }

    /**
     * Fetch user by ID
     */
    public User getUserById(Long id) {

        String sql = "SELECT id, name, email, role FROM users WHERE id = :id";

        return jdbcTemplate.queryForObject(
                sql,
                Map.of("id", id),
                userRowMapper
        );
    }
    
    public User findByEmail(String email) {

        String sql = "SELECT * FROM users WHERE email = :email";

        return jdbcTemplate.queryForObject(
                sql,
                Map.of("email", email),
                userRowMapper
        );
    }
    
    /**
     * Fetch all users
     */
    public List<User> getAllUsers() {

        String sql = "SELECT id, name, email, role FROM users";

        return jdbcTemplate.query(sql, userRowMapper);
    }
}