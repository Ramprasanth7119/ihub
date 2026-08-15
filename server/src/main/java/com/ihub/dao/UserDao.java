package com.ihub.dao;

import com.ihub.dto.UserRequest;
import com.ihub.mapper.UserRowMapper;
import com.ihub.model.User;
import org.springframework.dao.EmptyResultDataAccessException;
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
    
    /**
     * Looks a user up by email.
     *
     * @return the user, or {@code null} when no such account exists — callers treat
     *         a missing principal as an authentication failure rather than a 404,
     *         so this returns null instead of throwing.
     */
    public User findByEmail(String email) {

        String sql = "SELECT * FROM users WHERE email = :email";

        try {
            return jdbcTemplate.queryForObject(sql, Map.of("email", email), userRowMapper);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public boolean emailExists(String email) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE email = :email",
                Map.of("email", email),
                Integer.class
        );
        return count != null && count > 0;
    }

    /**
     * True when at least one administrator account exists.
     *
     * <p>Registration refuses to self-assign the ADMIN role, so a freshly migrated
     * database has no way in until one is seeded — see {@code AdminBootstrap}.</p>
     */
    public boolean adminExists() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE role = 'ADMIN'",
                Map.of(),
                Integer.class
        );
        return count != null && count > 0;
    }

    /** Inserts a pre-hashed account directly. Used only for the initial admin seed. */
    public Long createUserWithHash(String name, String email, String encodedPassword, String role) {
        String sql = """
            INSERT INTO users (name, email, password, role, verified, active, created_at)
            VALUES (:name, :email, :password, :role, true, true, NOW())
        """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("name", name)
                .addValue("email", email)
                .addValue("password", encodedPassword)
                .addValue("role", role);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, params, keyHolder, new String[]{"id"});
        return keyHolder.getKey() != null ? keyHolder.getKey().longValue() : null;
    }

    /**
     * Fetch a page of users. Admin-only at the controller layer — this exposes the
     * full account list.
     */
    public List<User> getAllUsers(int limit, int offset) {

        String sql = """
            SELECT id, name, email, role FROM users
            ORDER BY id DESC
            LIMIT :limit OFFSET :offset
        """;

        return jdbcTemplate.query(sql, Map.of("limit", limit, "offset", offset), userRowMapper);
    }

    public long countUsers() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Map.of(), Long.class);
        return count != null ? count : 0L;
    }

    /** Updates the profile fields a user is allowed to change about themselves. */
    public int updateProfile(Long userId, String name) {
        return jdbcTemplate.update(
                "UPDATE users SET name = :name WHERE id = :id",
                new MapSqlParameterSource()
                        .addValue("id", userId)
                        .addValue("name", name)
        );
    }

    public int updatePassword(Long userId, String encodedPassword) {
        return jdbcTemplate.update(
                "UPDATE users SET password = :password WHERE id = :id",
                new MapSqlParameterSource()
                        .addValue("id", userId)
                        .addValue("password", encodedPassword)
        );
    }

    /** Reads the stored password hash for verification during a password change. */
    public String findPasswordHash(Long userId) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT password FROM users WHERE id = :id",
                    Map.of("id", userId),
                    String.class
            );
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }
}