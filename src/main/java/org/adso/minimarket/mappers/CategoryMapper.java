package org.adso.minimarket.mappers;

import org.adso.minimarket.dto.CategorySummary;
import org.adso.minimarket.models.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    @Mapping(target = "parentName", expression = "java(category.getParent() != null ? category.getParent().getName() : null)")
    @Mapping(target = "fullPath", expression = "java(category.getFullPath())")
    CategorySummary toSummary(Category category);

    List<CategorySummary> toSummaryList(List<Category> allFeaturedCategories);
}
