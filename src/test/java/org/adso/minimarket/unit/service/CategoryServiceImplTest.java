package org.adso.minimarket.service;

import org.adso.minimarket.dto.CreateCategoryRequest;
import org.adso.minimarket.exception.NotFoundException;
import org.adso.minimarket.models.Category;
import org.adso.minimarket.repository.jpa.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
public class CategoryServiceImplTest {

    @InjectMocks
    private CategoryServiceImpl categoryService;

    @Mock
    private CategoryRepository categoryRepository;

    // ── getById ───────────────────────────────────────────────────────────────

    @Test
    void getById_whenFound_returnsCategory() {
        Category category = new Category(1L, "Ropa", List.of(), null);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        Category result = categoryService.getById(1L);

        assertEquals(1L, result.getId());
        assertEquals("Ropa", result.getName());
        verify(categoryRepository).findById(1L);
    }

    @Test
    void getById_whenNotFound_throwsNotFoundException() {
        when(categoryRepository.findById(any(Long.class))).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> categoryService.getById(99L));
        verify(categoryRepository).findById(99L);
    }

    // ── createCategory ────────────────────────────────────────────────────────

    @Test
    void createCategory_withoutParent_savesAndReturnsCategory() {
        CreateCategoryRequest req = new CreateCategoryRequest();
        ReflectionTestUtils.setField(req, "name", "Electronica");
        ReflectionTestUtils.setField(req, "attributeDefinitions", List.of());
        ReflectionTestUtils.setField(req, "parentId", null);

        Category saved = new Category("Electronica", List.of(), null);
        ReflectionTestUtils.setField(saved, "id", 2L);

        when(categoryRepository.save(any(Category.class))).thenReturn(saved);

        Category result = categoryService.createCategory(req);

        assertEquals("Electronica", result.getName());
        assertNull(result.getParent());
        verify(categoryRepository).save(any(Category.class));
        verify(categoryRepository, never()).findById(any());
    }

    @Test
    void createCategory_withParent_loadsParentAndSaves() {
        Category parent = new Category(1L, "Ropa", List.of(), null);

        CreateCategoryRequest req = new CreateCategoryRequest();
        ReflectionTestUtils.setField(req, "name", "Camisetas");
        ReflectionTestUtils.setField(req, "attributeDefinitions", List.of());
        ReflectionTestUtils.setField(req, "parentId", 1L);

        Category saved = new Category("Camisetas", List.of(), parent);
        ReflectionTestUtils.setField(saved, "id", 3L);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(categoryRepository.save(any(Category.class))).thenReturn(saved);

        Category result = categoryService.createCategory(req);

        assertEquals("Camisetas", result.getName());
        assertNotNull(result.getParent());
        assertEquals("Ropa", result.getParent().getName());
        verify(categoryRepository).findById(1L);
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void createCategory_withInvalidParent_throwsNotFoundException() {
        CreateCategoryRequest req = new CreateCategoryRequest();
        ReflectionTestUtils.setField(req, "name", "Subcategoria");
        ReflectionTestUtils.setField(req, "attributeDefinitions", List.of());
        ReflectionTestUtils.setField(req, "parentId", 999L);

        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> categoryService.createCategory(req));
        verify(categoryRepository, never()).save(any());
    }

    // ── getAllFeaturedCategories ───────────────────────────────────────────────

    @Test
    void getAllFeaturedCategories_returnsUpToFour() {
        List<Category> featured = List.of(
                new Category(1L, "Ropa", List.of(), null),
                new Category(2L, "Electro", List.of(), null),
                new Category(3L, "Hogar", List.of(), null),
                new Category(4L, "Deportes", List.of(), null)
        );

        when(categoryRepository.findTop4ByOrderByIdAsc()).thenReturn(featured);

        List<Category> result = categoryService.getAllFeaturedCategories();

        assertEquals(4, result.size());
        verify(categoryRepository).findTop4ByOrderByIdAsc();
    }

    @Test
    void getAllFeaturedCategories_whenEmpty_returnsEmptyList() {
        when(categoryRepository.findTop4ByOrderByIdAsc()).thenReturn(List.of());

        List<Category> result = categoryService.getAllFeaturedCategories();

        assertTrue(result.isEmpty());
    }
}
