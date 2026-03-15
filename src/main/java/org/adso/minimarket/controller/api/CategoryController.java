package org.adso.minimarket.controller.api;

import jakarta.validation.Valid;
import org.adso.minimarket.dto.CategorySummary;
import org.adso.minimarket.dto.CreateCategoryRequest;
import org.adso.minimarket.mappers.CategoryMapper;
import org.adso.minimarket.service.CategoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class CategoryController {

    private final CategoryService categoryService;
    private final CategoryMapper categoryMapper;

    public CategoryController(CategoryService categoryService, CategoryMapper categoryMapper) {
        this.categoryService = categoryService;
        this.categoryMapper = categoryMapper;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @RequestMapping("/admin/categories")
    public ResponseEntity<CategorySummary> createCategory(@RequestBody @Valid CreateCategoryRequest request) {
        return new ResponseEntity<>(
                categoryMapper.toSummary(categoryService.createCategory(request)),
                HttpStatus.CREATED
        );
    }

    @GetMapping
    @RequestMapping("/categories/featured")
    public ResponseEntity<List<CategorySummary>> getFeaturedCategories() {
        return new ResponseEntity<>(
                categoryMapper.toSummaryList(categoryService.getAllFeaturedCategories()),
                HttpStatus.OK
        );
    }
}
