package org.adso.minimarket.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DetailedProduct {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private String price;
    private CategorySummary category;
    private String brand;
    private int stock;
    private List<String> images = new ArrayList<>();
    @JsonProperty(value = "listedAt")
    private LocalDateTime createdAt;
}
