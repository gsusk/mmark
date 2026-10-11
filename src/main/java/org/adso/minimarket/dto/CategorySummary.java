package org.adso.minimarket.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategorySummary {
    private Long id;

    @JsonProperty("categoryName")
    private String name;

    private String parentName;

    private String fullPath;

    private List<Map<String, Object>> attributeDefinitions;
}
