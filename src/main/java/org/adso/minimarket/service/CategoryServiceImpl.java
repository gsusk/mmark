package org.adso.minimarket.service;

import org.adso.minimarket.exception.NotFoundException;
import org.adso.minimarket.models.Category;
import org.adso.minimarket.repository.jpa.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public Category getById(Long id) {
        return this.categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found"));
    }

    @Override
    public Category createCategory(org.adso.minimarket.dto.CreateCategoryRequest request) {
        Category parent = null;
        if (request.getParentId() != null) {
            parent = getById(request.getParentId());
        }
        return this.categoryRepository.save(new Category(request.getName(), request.getAttributeDefinitions(), parent));
    }

    @Override
    public List<Category> getAllFeaturedCategories() {
        return this.categoryRepository.findTop4ByOrderByIdAsc();
    }
}
