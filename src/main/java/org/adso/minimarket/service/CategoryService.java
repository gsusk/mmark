package org.adso.minimarket.service;

import org.adso.minimarket.models.Category;

import java.util.List;

public interface CategoryService {
    Category getById(Long id);

    Category createCategory(org.adso.minimarket.dto.CreateCategoryRequest request);

    List<Category> getAllFeaturedCategories();
}
