package org.adso.minimarket.models.document;

import lombok.*;
import org.adso.minimarket.dto.ProductCard;
// import org.springframework.data.annotation.Id;
// import org.springframework.data.elasticsearch.annotations.DateFormat;
// import org.springframework.data.elasticsearch.annotations.Document;
// import org.springframework.data.elasticsearch.annotations.Field;
// import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// @Document(indexName = "products")
public class ProductDocument {

    // @Id
    private Long id;

    // @Field(type = FieldType.Text, analyzer = "standard")
    private String name;

    private String slug;

    // @Field(type = FieldType.Text, analyzer = "standard")
    private String description;

    // @Field(type = FieldType.Keyword)
    private String category;

    // @Field(type = FieldType.Double)
    private BigDecimal price;

    // @Field(type = FieldType.Keyword)
    private String brand;

    // @Field(type = FieldType.Integer)
    private Integer stock;

    // @Field(type = FieldType.Nested)
    private Map<String, Object> specifications;

    // @Field(type = FieldType.Keyword)
    private List<String> images;

    // @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime createdAt;


    public ProductCard toProductCard() {
        return new ProductCard(
                id,
                name,
                slug,
                category,
                price,
                brand,
                createdAt,
                images
        );
    }
}