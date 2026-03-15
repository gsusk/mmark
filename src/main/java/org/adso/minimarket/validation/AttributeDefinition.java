package org.adso.minimarket.validation;

import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
public class AttributeDefinition {
    private final String name;
    private final AttributeType type;
    private final boolean required;
    private final boolean facetable;
    private final FilterType filterType;
    private final FacetStrategy facetStrategy;
    private final CardinalityHint cardinalityHint;

    private final List<?> options;
    private final Number min;
    private final Number max;

    public AttributeDefinition(String name, AttributeType type, boolean required, boolean facetable,
                               FilterType filterType, FacetStrategy facetStrategy, CardinalityHint cardinalityHint,
                               List<?> options, Number min, Number max) {
        this.name = name;
        this.type = type;
        this.required = required;
        this.facetable = facetable;
        this.filterType = filterType;
        this.facetStrategy = facetStrategy;
        this.cardinalityHint = cardinalityHint;
        this.options = options;
        this.min = min;
        this.max = max;
    }

    public static AttributeDefinition fromMap(Map<String, Object> map) {
        String name = (String) map.get("name");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Attribute definition must have a name");
        }

        String typeStr = (String) map.get("type");
        if (typeStr == null || typeStr.isBlank()) {
            throw new IllegalArgumentException("Attribute definition must have a type");
        }
        AttributeType type = AttributeType.fromString(typeStr);

        boolean required = map.containsKey("required") ? (Boolean) map.get("required") : false;
        boolean facetable = map.containsKey("facetable") ? (Boolean) map.get("facetable") : false;

        FilterType filterType = parseFilterType(map, type);
        FacetStrategy facetStrategy = parseFacetStrategy(map, facetable);
        CardinalityHint cardinalityHint = parseCardinalityHint(map);

        List<?> options = map.containsKey("options") ? (List<?>) map.get("options") : null;
        Number min = map.containsKey("min") ? (Number) map.get("min") : null;
        Number max = map.containsKey("max") ? (Number) map.get("max") : null;

        return new AttributeDefinition(name, type, required, facetable,
                filterType, facetStrategy, cardinalityHint,
                options, min, max);
    }

    private static FilterType parseFilterType(Map<String, Object> map, AttributeType type) {
        if (map.containsKey("filterType")) {
            return FilterType.fromString((String) map.get("filterType"));
        }
        
        return switch (type) {
            case NUMBER -> FilterType.RANGE;
            case BOOLEAN -> FilterType.BOOLEAN;
            case STRING -> FilterType.TERM;
            case ENUM -> FilterType.MULTI_SELECT;
        };
    }

    private static FacetStrategy parseFacetStrategy(Map<String, Object> map, boolean facetable) {
        if (map.containsKey("facetStrategy")) {
            return FacetStrategy.fromString((String) map.get("facetStrategy"));
        }

        if (facetable) {
            CardinalityHint hint = parseCardinalityHint(map);
            return hint.suggestFacetStrategy();
        }
        
        return FacetStrategy.NONE;
    }

    private static CardinalityHint parseCardinalityHint(Map<String, Object> map) {
        if (map.containsKey("cardinalityHint")) {
            return CardinalityHint.fromString((String) map.get("cardinalityHint"));
        }
        return CardinalityHint.UNKNOWN;
    }

    public boolean hasOptions() {
        return options != null && !options.isEmpty();
    }

    public boolean hasMin() {
        return min != null;
    }

    public boolean hasMax() {
        return max != null;
    }

    public boolean isFilterable() {
        return filterType != null && filterType.isFilterable();
    }

    public boolean isFacetable() {
        return facetStrategy != null && facetStrategy.isFacetable();
    }
}
