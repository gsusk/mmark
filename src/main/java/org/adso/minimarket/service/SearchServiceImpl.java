package org.adso.minimarket.service;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.AggregationBuilders;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import lombok.extern.slf4j.Slf4j;
import org.adso.minimarket.dto.FacetValue;
import org.adso.minimarket.dto.ProductCard;
import org.adso.minimarket.dto.SearchFilters;
import org.adso.minimarket.dto.SearchResult;
import org.adso.minimarket.models.document.ProductDocument;
import org.adso.minimarket.repository.elastic.ProductSearchRepository;
import org.adso.minimarket.validation.AttributeDefinition;
import org.adso.minimarket.validation.AttributeSchemaValidator;
import org.adso.minimarket.validation.FacetStrategy;
import org.adso.minimarket.validation.FilterType;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

@Slf4j
@Service
public class SearchServiceImpl implements SearchService {
    private static final String FIELD_PRICE = "price";
    private static final String FIELD_BRAND = "brand";
    private static final String FIELD_CATEGORY = "category";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_DESCRIPTION = "description";
    private static final String FIELD_SPECIFICATIONS = "specifications";
    private static final String KEYWORD_SUFFIX = ".keyword";
    private static final String AGG_MIN_PRICE = "min_price";
    private static final String AGG_MAX_PRICE = "max_price";
    private static final String FUZZINESS_AUTO = "AUTO";
    private static final int MAX_RESULTS = 20;
    private static final int BRAND_FACET_SIZE = 10;

    private final ProductSearchRepository searchRepository;
    private final ElasticsearchOperations operations;
    private final AttributeSchemaValidator attributeSchemaValidator;

    public SearchServiceImpl(ProductSearchRepository searchRepository, ElasticsearchOperations operations,
                             AttributeSchemaValidator attributeSchemaValidator) {
        this.searchRepository = searchRepository;
        this.operations = operations;
        this.attributeSchemaValidator = attributeSchemaValidator;
    }

    @Override
    public void saveIndex(ProductDocument product) {
        searchRepository.save(product);
    }

    @Override
    public SearchResult searchWithFilters(SearchFilters filters, String query) {
        sanitizeFilters(filters);
        NativeQuery searchQuery = buildQuery(filters, query);
        SearchHits<ProductDocument> searchHits = operations.search(searchQuery, ProductDocument.class);

        return SearchResult.builder()
                .products(extractProducts(searchHits))
                .minPrice(extractMinPrice(searchHits))
                .maxPrice(extractMaxPrice(searchHits))
                .facets(extractFacets(searchHits, filters.getCategory()))
                .total(searchHits.getTotalHits())
                .build();
    }

    private List<ProductCard> extractProducts(SearchHits<ProductDocument> searchHits) {
        return searchHits.getSearchHits().stream()
                .map(doc -> doc.getContent().toProductCard())
                .toList();
    }

    private NativeQuery buildQuery(SearchFilters filters, String query) {
        BoolQuery boolQuery = buildBoolQuery(filters, query);
        BoolQuery postFilterQuery = buildPostFilterQuery(filters);

        NativeQueryBuilder nqBuilder = NativeQuery.builder()
                .withQuery(q -> q.bool(boolQuery))
                .withAggregation(AGG_MIN_PRICE, AggregationBuilders.min(m -> m.field(FIELD_PRICE)))
                .withAggregation(AGG_MAX_PRICE, AggregationBuilders.max(m -> m.field(FIELD_PRICE)))
                .withMaxResults(MAX_RESULTS);

        addFacetAggregations(nqBuilder, filters.getCategory());

        if (hasFilters(postFilterQuery)) {
            nqBuilder.withFilter(f -> f.bool(postFilterQuery));
        }

        return nqBuilder.build();
    }

    private BoolQuery buildBoolQuery(SearchFilters filters, String query) {
        BoolQuery.Builder bool = new BoolQuery.Builder()
                .must(mu -> mu.bool(b -> b
                        .should(s -> s.multiMatch(m -> m
                                .fields(FIELD_NAME + "^3", FIELD_DESCRIPTION + "^1")
                                .query(query)
                                .fuzziness(FUZZINESS_AUTO)
                                .type(TextQueryType.BestFields)))
                        .should(s -> s.prefix(p -> p
                                .field(FIELD_NAME)
                                .value(query.toLowerCase())))));

        if (isNotBlank(filters.getCategory())) {
            bool.filter(f -> f.term(t -> t
                    .field(FIELD_CATEGORY)
                    .value(filters.getCategory())
                    .caseInsensitive(true)));
        }

        return bool.build();
    }

    private BoolQuery buildPostFilterQuery(SearchFilters filters) {
        BoolQuery.Builder postFilter = new BoolQuery.Builder();

        applyPriceFilters(postFilter, filters);
        applyBrandFilter(postFilter, filters);
        applyAttributeFilters(postFilter, filters);

        return postFilter.build();
    }

    private void applyPriceFilters(BoolQuery.Builder postFilter, SearchFilters filters) {
        if (isValidPrice(filters.getMinPrice())) {
            postFilter.filter(f -> f.range(r -> r.number(n -> n
                    .field(FIELD_PRICE)
                    .gte(filters.getMinPrice().doubleValue()))));
        }

        if (isValidPrice(filters.getMaxPrice())) {
            postFilter.filter(f -> f.range(r -> r.number(n -> n
                    .field(FIELD_PRICE)
                    .lte(filters.getMaxPrice().doubleValue()))));
        }
    }

    private void applyBrandFilter(BoolQuery.Builder postFilter, SearchFilters filters) {
        if (isNotBlank(filters.getBrand())) {
            postFilter.filter(f -> f.term(t -> t
                    .field(FIELD_BRAND)
                    .value(filters.getBrand())
                    .caseInsensitive(true)));
        }
    }

    private void applyAttributeFilters(BoolQuery.Builder postFilter, SearchFilters filters) {
        if (filters.getAttributes() == null || filters.getAttributes().isEmpty()) {
            return;
        }

        filters.getAttributes().entrySet().stream()
                .filter(entry -> isNotBlank(entry.getValue()))
                .forEach(entry -> {
                    FilterType filterType = attributeSchemaValidator.getFilterType(
                            filters.getCategory(), entry.getKey());
                    applyAttributeFilter(postFilter, entry.getKey(), entry.getValue(), filterType);
                });
    }

    private void applyAttributeFilter(BoolQuery.Builder postFilter, String attrName, String attrValue,
                                      FilterType filterType) {
        String fieldPath = FIELD_SPECIFICATIONS + "." + attrName;

        switch (filterType) {
            case TERM -> applyTermFilter(postFilter, fieldPath, attrValue);
            case MULTI_SELECT -> applyMultiSelectFilter(postFilter, fieldPath, attrValue);
            case BOOLEAN -> applyBooleanFilter(postFilter, fieldPath, attrValue);
            case RANGE -> applyRangeFilter(postFilter, fieldPath, attrValue, attrName);
            default -> log.warn("Attribute '{}' has filter type NONE, skipping", attrName);
        }
    }

    private void applyTermFilter(BoolQuery.Builder postFilter, String fieldPath, String value) {
        postFilter.filter(f -> f.term(t -> t
                .field(fieldPath + KEYWORD_SUFFIX)
                .value(value)
                .caseInsensitive(true)));
    }

    private void applyMultiSelectFilter(BoolQuery.Builder postFilter, String fieldPath, String value) {
        String[] values = value.split(",");
        String keywordField = fieldPath + KEYWORD_SUFFIX;

        if (values.length == 1) {
            postFilter.filter(f -> f.term(t -> t
                    .field(keywordField)
                    .value(values[0].trim())
                    .caseInsensitive(true)));
        } else {
            List<FieldValue> fieldValues = Arrays.stream(values)
                    .map(String::trim)
                    .map(FieldValue::of)
                    .toList();
            postFilter.filter(f -> f.terms(t -> t
                    .field(keywordField)
                    .terms(ts -> ts.value(fieldValues))));
        }
    }

    private void applyBooleanFilter(BoolQuery.Builder postFilter, String fieldPath, String value) {
        postFilter.filter(f -> f.term(t -> t
                .field(fieldPath)
                .value(Boolean.parseBoolean(value))));
    }

    private void applyRangeFilter(BoolQuery.Builder postFilter, String fieldPath, String value, String attrName) {
        try {
            if (value.contains("-")) {
                String[] parts = value.split("-");
                double min = Double.parseDouble(parts[0].trim());
                double max = Double.parseDouble(parts[1].trim());
                postFilter.filter(f -> f.range(r -> r.number(n -> n
                        .field(fieldPath)
                        .gte(min)
                        .lte(max))));
            } else {
                double numValue = Double.parseDouble(value.trim());
                postFilter.filter(f -> f.term(t -> t
                        .field(fieldPath)
                        .value(numValue)));
            }
        } catch (NumberFormatException e) {
            log.warn("Invalid range value for attribute '{}': {}", attrName, value);
        }
    }

    private void addFacetAggregations(NativeQueryBuilder nq, String category) {
        nq.withAggregation(FIELD_BRAND, AggregationBuilders.terms(t -> t
                .field(FIELD_BRAND)
                .size(BRAND_FACET_SIZE)));

        attributeSchemaValidator.getAttributeDefinitions(category).values().stream()
                .filter(AttributeDefinition::isFacetable)
                .forEach(def -> {
                    String fieldPath = FIELD_SPECIFICATIONS + "." + def.getName() + KEYWORD_SUFFIX;
                    Aggregation agg = buildFacetAggregation(fieldPath, def.getFacetStrategy());
                    if (agg != null) {
                        nq.withAggregation(def.getName(), agg);
                    }
                });
    }

    private Aggregation buildFacetAggregation(String fieldPath, FacetStrategy strategy) {
        return switch (strategy) {
            case TERMS -> AggregationBuilders.terms(t -> t
                    .field(fieldPath)
                    .size(strategy.getDefaultSize()));
            case SIGNIFICANT_TERMS -> AggregationBuilders.significantTerms(st -> st
                    .field(fieldPath)
                    .size(strategy.getDefaultSize()));
            case SAMPLER -> AggregationBuilders.sampler(s -> s
                    .shardSize(strategy.getSampleSize()));
            case NONE -> null;
        };
    }

    private Map<String, List<FacetValue>> extractFacets(SearchHits<ProductDocument> searchHits,
                                                        String category) {
        Map<String, List<FacetValue>> facets = new HashMap<>();

        ElasticsearchAggregations agg = (ElasticsearchAggregations) searchHits.getAggregations();
        if (agg == null) return facets;

        try {
            extractTermsFacet(agg, FIELD_BRAND, facets);

            attributeSchemaValidator.getAttributeDefinitions(category).values().stream()
                    .filter(AttributeDefinition::isFacetable)
                    .forEach(def -> extractFacetByStrategy(agg, def.getName(), def.getFacetStrategy(), facets));
        } catch (Exception e) {
            log.warn("Failed to extract facets: {}", e.getMessage());
        }

        return facets;
    }

    private void extractFacetByStrategy(ElasticsearchAggregations agg, String facetName,
                                        FacetStrategy strategy, Map<String, List<FacetValue>> facets) {
        switch (strategy) {
            case TERMS, SIGNIFICANT_TERMS -> extractTermsFacet(agg, facetName, facets);
            case SAMPLER -> extractSamplerFacet(agg, facetName, facets);
            case NONE -> {
            }
        }
    }

    private void extractTermsFacet(ElasticsearchAggregations agg, String facetName,
                                   Map<String, List<FacetValue>> facets) {
        try {
            Aggregate aggregate = getAggregate(agg, facetName);
            if (aggregate == null) return;

            List<FacetValue> values = extractFacetValues(aggregate);
            if (!values.isEmpty()) {
                facets.put(facetName, values);
            }
        } catch (Exception e) {
            log.debug("Failed to extract terms facet '{}': {}", facetName, e.getMessage());
        }
    }

    private List<FacetValue> extractFacetValues(Aggregate aggregate) {
        Stream<FacetValue> facetStream = null;

        if (aggregate.isSterms()) {
            facetStream = aggregate.sterms().buckets().array().stream()
                    .map(b -> new FacetValue(b.key().stringValue(), b.docCount()));
        } else if (aggregate.isLterms()) {
            facetStream = aggregate.lterms().buckets().array().stream()
                    .map(b -> new FacetValue(String.valueOf(b.key()), b.docCount()));
        } else if (aggregate.isDterms()) {
            facetStream = aggregate.dterms().buckets().array().stream()
                    .map(b -> new FacetValue(String.valueOf(b.key()), b.docCount()));
        } else if (aggregate.isSigsterms()) {
            facetStream = aggregate.sigsterms().buckets().array().stream()
                    .map(b -> new FacetValue(b.key(), b.docCount()));
        }

        return facetStream != null
                ? facetStream.filter(fv -> fv.getCount() > 0).toList()
                : Collections.emptyList();
    }

    private void extractSamplerFacet(ElasticsearchAggregations agg, String facetName,
                                     Map<String, List<FacetValue>> facets) {
        try {
            Aggregate samplerAgg = getAggregate(agg, facetName);
            if (samplerAgg == null || !samplerAgg.isSampler()) return;

            Map<String, Aggregate> subAggs = samplerAgg.sampler().aggregations();
            Aggregate termsAgg = subAggs.get("sampled_terms");

            if (termsAgg != null && termsAgg.isSterms()) {
                List<FacetValue> values = termsAgg.sterms().buckets().array().stream()
                        .map(b -> new FacetValue(b.key().stringValue(), b.docCount()))
                        .filter(fv -> fv.getCount() > 0)
                        .toList();
                if (!values.isEmpty()) {
                    facets.put(facetName, values);
                }
            }
        } catch (Exception e) {
            log.debug("Failed to extract sampler facet '{}': {}", facetName, e.getMessage());
        }
    }

    private BigDecimal extractMinPrice(SearchHits<ProductDocument> searchHits) {
        return extractAggregationValue(searchHits, AGG_MIN_PRICE, Aggregate::min);
    }

    private BigDecimal extractMaxPrice(SearchHits<ProductDocument> searchHits) {
        return extractAggregationValue(searchHits, AGG_MAX_PRICE, Aggregate::max);
    }

    private BigDecimal extractAggregationValue(SearchHits<ProductDocument> searchHits, String aggName,
                                               Function<Aggregate, ?> extractor) {
        try {
            ElasticsearchAggregations aggs = (ElasticsearchAggregations) searchHits.getAggregations();
            Aggregate aggregate = aggs.get(aggName).aggregation().getAggregate();

            Double value = aggName.equals(AGG_MIN_PRICE)
                    ? aggregate.min().value()
                    : aggregate.max().value();

            return isValidDouble(value)
                    ? BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP)
                    : null;
        } catch (Exception e) {
            log.warn("Failed to extract aggregation '{}': {}", aggName, e.getMessage());
            return null;
        }
    }

    private void sanitizeFilters(SearchFilters filters) {
        if (filters.getAttributes() == null || filters.getAttributes().isEmpty()) {
            return;
        }

        Set<String> filterableAttributes = attributeSchemaValidator.getFilterableAttributes(filters.getCategory());
        filters.getAttributes().keySet().retainAll(filterableAttributes);

        if (filters.getAttributes().isEmpty()) {
            log.debug("All attribute filters were invalid for category: {}", filters.getCategory());
        }
    }

    private Aggregate getAggregate(ElasticsearchAggregations agg, String name) {
        return agg.get(name) != null ? agg.get(name).aggregation().getAggregate() : null;
    }

    private boolean hasFilters(BoolQuery query) {
        return !query.filter().isEmpty();
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private boolean isValidPrice(BigDecimal price) {
        return price != null && price.compareTo(BigDecimal.ZERO) > 0;
    }

    private boolean isValidDouble(Double value) {
        return value != null && !value.isNaN() && !value.isInfinite();
    }
}
