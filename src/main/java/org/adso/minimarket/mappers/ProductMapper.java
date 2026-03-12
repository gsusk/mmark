package org.adso.minimarket.mappers;

import org.adso.minimarket.dto.DetailedProduct;
import org.adso.minimarket.models.product.Image;
import org.adso.minimarket.models.product.Product;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ProductMapper {
    @Mapping(
            target = "price",
            expression = "java(product.getPrice().toPlainString())"
    )
    @Mapping(target = "category", source = "category")
    @Mapping(target = "brand", source = "brand")
    @Mapping(target = "slug", source = "slug")
    DetailedProduct toDto(Product product);

    @AfterMapping
    default void updateCategoryInfo(Product product, @MappingTarget DetailedProduct dto) {
        if (product.getCategory() != null && dto.getCategory() != null) {
            dto.getCategory().setFullPath(product.getCategory().getFullPath());
            dto.getCategory().setParentName(
                    product.getCategory().getParent() != null ? product.getCategory().getParent().getName() : null
            );
        }
    }

    default String map(Image image) {
        return image == null ? null : image.getUrl();
    }
}
