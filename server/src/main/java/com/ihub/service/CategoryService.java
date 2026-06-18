package com.ihub.service;

import com.ihub.dao.CategoryDao;
import com.ihub.dto.CategoryResponse;
import com.ihub.model.Category;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryDao categoryDao;

    public CategoryService(CategoryDao categoryDao) {
        this.categoryDao = categoryDao;
    }

    public List<CategoryResponse> getAllCategories() {
        return categoryDao.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public void validateCategorySlug(String slug) {
        if (!categoryDao.existsBySlug(slug)) {
            throw new com.ihub.exception.CustomException("Invalid category: " + slug);
        }
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getSlug());
    }
}
