package com.ihub.dao;

import com.ihub.mapper.CategoryRowMapper;
import com.ihub.model.Category;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class CategoryDao {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final CategoryRowMapper categoryRowMapper;

    public CategoryDao(NamedParameterJdbcTemplate jdbcTemplate, CategoryRowMapper categoryRowMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.categoryRowMapper = categoryRowMapper;
    }

    public List<Category> findAll() {
        return jdbcTemplate.query(
                "SELECT id, name, slug FROM categories ORDER BY name",
                categoryRowMapper
        );
    }

    public boolean existsBySlug(String slug) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM categories WHERE slug = :slug",
                Map.of("slug", slug.toLowerCase()),
                Integer.class
        );
        return count != null && count > 0;
    }

    public Category findBySlug(String slug) {
        return jdbcTemplate.queryForObject(
                "SELECT id, name, slug FROM categories WHERE slug = :slug",
                Map.of("slug", slug.toLowerCase()),
                categoryRowMapper
        );
    }
}
