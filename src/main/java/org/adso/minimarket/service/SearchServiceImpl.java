package org.adso.minimarket.service;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
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
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
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
        Query baseQuery = buildBaseQuery(filters.getCategory(), query);
        Map<String, Query> filterMap = buildFilterMap(filters);

        NativeQueryBuilder nqBuilder = NativeQuery.builder()
                .withQuery(baseQuery)
                .withMaxResults(MAX_RESULTS);

        if (!filterMap.isEmpty()) {
            // Aplicamos los filtros DESPUES de la busqueda global usando un post-filter.
            // Asi las estadisticas como min_price/max_price o los facets de categorias se pueden 
            // calcular de manera correcta sin ser removidos por el propio filtro (no achicar el slider de precio).
            BoolQuery.Builder postFilterBool = new BoolQuery.Builder();
            filterMap.values().forEach(postFilterBool::filter);
            nqBuilder.withFilter(Query.of(q -> q.bool(postFilterBool.build())));
        }

        Aggregation priceAgg = buildFilterAgg(FIELD_PRICE, null, 0, filterMap, true);
        nqBuilder.withAggregation("price_stats", priceAgg);

        addAdvancedFacetAggregations(nqBuilder, filters, filterMap);

        return nqBuilder.build();
    }

    private Query buildBaseQuery(String category, String queryStr) {
        BoolQuery.Builder base = new BoolQuery.Builder();

        if (StringUtils.hasText(queryStr)) {
            base.must(mu -> mu.bool(b -> b
                    .should(s -> s.multiMatch(m -> m
                            .fields(FIELD_NAME + "^3", FIELD_DESCRIPTION + "^1")
                            .query(queryStr)
                            .fuzziness(FUZZINESS_AUTO)
                            .type(TextQueryType.BestFields)))
                    .should(s -> s.prefix(p -> p
                            .field(FIELD_NAME)
                            .value(queryStr.toLowerCase())))));
        } else {
            base.must(m -> m.matchAll(a -> a));
        }

        if (StringUtils.hasText(category)) {
            List<String> categoriesToSearch = attributeSchemaValidator.getAllDescendantNames(category);

            base.filter(f -> f.terms(t -> t
                    .field(FIELD_CATEGORY)
                    .terms(ts -> ts.value(categoriesToSearch.stream().map(FieldValue::of).toList()))));
        }

        return base.build()._toQuery();
    }

    private Map<String, Query> buildFilterMap(SearchFilters filters) {
        Map<String, Query> filterMap = new HashMap<>();

        if (isValidPrice(filters.getMinPrice()) || isValidPrice(filters.getMaxPrice())) {
            filterMap.put(FIELD_PRICE, buildPriceQuery(filters)._toQuery());
        }

        if (StringUtils.hasText(filters.getBrand())) {
            filterMap.put(FIELD_BRAND,
                    Query.of(q -> q.term(t -> t.field(FIELD_BRAND).value(filters.getBrand()).caseInsensitive(true))));
        }

        if (filters.getAttributes() != null) {
            filters.getAttributes().entrySet().stream()
                    .filter(entry -> StringUtils.hasText(entry.getValue()))
                    .forEach(entry -> {
                        String attrName = entry.getKey();
                        FilterType type = attributeSchemaValidator.getFilterType(filters.getCategory(), attrName);
                        Query attrQuery = buildNestedAttributeQuery(attrName, entry.getValue(), type);
                        if (attrQuery != null) {
                            filterMap.put(attrName, attrQuery);
                        }
                    });
        }

        return filterMap;
    }

    private RangeQuery buildPriceQuery(SearchFilters filters) {
        return RangeQuery.of(r -> {
            r.number(n -> {
                n.field(FIELD_PRICE);
                if (isValidPrice(filters.getMinPrice())) {
                    n.gte(filters.getMinPrice().doubleValue());
                }
                if (isValidPrice(filters.getMaxPrice())) {
                    n.lte(filters.getMaxPrice().doubleValue());
                }
                return n;
            });
            return r;
        });
    }

    private Query buildNestedAttributeQuery(String attrName, String attrValue, FilterType filterType) {
        BoolQuery.Builder innerBq = new BoolQuery.Builder();
        String fieldPath = FIELD_SPECIFICATIONS + "." + attrName;

        switch (filterType) {
            case TERM -> applyTermFilter(innerBq, fieldPath, attrValue);
            case MULTI_SELECT -> applyMultiSelectFilter(innerBq, fieldPath, attrValue);
            case BOOLEAN -> applyBooleanFilter(innerBq, fieldPath, attrValue);
            case RANGE -> applyRangeFilter(innerBq, fieldPath, attrValue, attrName);
            default -> {
                return null;
            }
        }

        List<Query> innerFilters = innerBq.build().filter();
        if (innerFilters.isEmpty()) return null;

        return Query.of(q -> q.nested(n -> n
                .path(FIELD_SPECIFICATIONS)
                .query(innerFilters.get(0))
        ));
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
                postFilter.filter(f -> f.range(r -> r.number(n -> n.field(fieldPath).gte(min).lte(max))));
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

    private void addAdvancedFacetAggregations(NativeQueryBuilder nq, SearchFilters filters,
                                              Map<String, Query> activeFiltersMap) {
        Aggregation brandAgg = buildFilterAgg(FIELD_BRAND, FIELD_BRAND, BRAND_FACET_SIZE, activeFiltersMap, false);
        nq.withAggregation(FIELD_BRAND, brandAgg);

        Aggregation categoryAgg = buildFilterAgg(FIELD_CATEGORY, FIELD_CATEGORY, 5, activeFiltersMap, false);
        nq.withAggregation(FIELD_CATEGORY, categoryAgg);

        attributeSchemaValidator.getAttributeDefinitions(filters.getCategory()).values().stream()
                .filter(AttributeDefinition::isFacetable)
                .forEach(def -> {
                    String fieldPath = FIELD_SPECIFICATIONS + "." + def.getName() + KEYWORD_SUFFIX;
                    Aggregation agg = buildNestedFilterAggForStrategy(def.getName(), fieldPath,
                            def.getFacetStrategy(), activeFiltersMap);
                    if (agg != null) {
                        nq.withAggregation(def.getName(), agg);
                    }
                });
    }

    private Aggregation buildFilterAgg(String filterKeyToIgnore, String fieldTermToAggregate, int size, Map<String,
            Query> activeFiltersMap, boolean isPriceStats) {
        BoolQuery.Builder filterBool = new BoolQuery.Builder();

        // Ignoramos especificamente el propio filtro para que las opciones hermanas no desaparezcan en el lado del
        // frontend.
        // Ej: si das click en "Marca = Samsung", el sidebar igual mostrara "LG" y "Sony" agregando un matchAll
        // condicional.
        activeFiltersMap.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(filterKeyToIgnore))
                .forEach(entry -> filterBool.filter(entry.getValue()));

        Query filterQuery = activeFiltersMap.size() <= (activeFiltersMap.containsKey(filterKeyToIgnore) ? 1 : 0)
                ? Query.of(q -> q.matchAll(m -> m))
                : Query.of(q -> q.bool(filterBool.build()));

        if (isPriceStats) {
            return Aggregation.of(a -> a
                    .filter(filterQuery)
                    .aggregations(AGG_MIN_PRICE, Aggregation.of(ia -> ia.min(m -> m.field(FIELD_PRICE))))
                    .aggregations(AGG_MAX_PRICE, Aggregation.of(ia -> ia.max(m -> m.field(FIELD_PRICE))))
            );
        }

        return Aggregation.of(a -> a
                .filter(filterQuery)
                .aggregations("filtered_terms",
                        Aggregation.of(ia -> ia.terms(t -> t.field(fieldTermToAggregate).size(size))))
        );
    }

    private Aggregation buildNestedFilterAggForStrategy(String filterKeyToIgnore, String fieldPath,
                                                        FacetStrategy strategy, Map<String, Query> activeFiltersMap) {
        BoolQuery.Builder filterBool = new BoolQuery.Builder();
        activeFiltersMap.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(filterKeyToIgnore))
                .forEach(entry -> filterBool.filter(entry.getValue()));

        Query filterQuery = activeFiltersMap.size() <= (activeFiltersMap.containsKey(filterKeyToIgnore) ? 1 : 0)
                ? Query.of(q -> q.matchAll(m -> m))
                : Query.of(q -> q.bool(filterBool.build()));

        // Como los atributos dinamicos estan encapsulados dentro de una lista 'nested' llamada 'specifications',
        // toca armar una agregacion tipo nested y evaluar la estrategia de agrupe (Terms, Sampler, etc) desde adentro.
        Aggregation internalAgg;
        switch (strategy) {
            case TERMS ->
                    internalAgg = Aggregation.of(a -> a.terms(t -> t.field(fieldPath).size(strategy.getDefaultSize())));
            case SIGNIFICANT_TERMS -> internalAgg =
                    Aggregation.of(a -> a.significantTerms(st -> st.field(fieldPath).size(strategy.getDefaultSize())));
            case SAMPLER -> {
                Aggregation sampledTerms =
                        Aggregation.of(a -> a.terms(t -> t.field(fieldPath).size(strategy.getDefaultSize())));
                internalAgg = Aggregation.of(a -> a.sampler(s -> s.shardSize(strategy.getSampleSize())).aggregations(
                        "sampled_terms", sampledTerms));
            }
            default -> {
                return null;
            }
        }

        return Aggregation.of(a -> a
                .filter(filterQuery)
                .aggregations("nested_aggs", Aggregation.of(na -> na
                        .nested(n -> n.path(FIELD_SPECIFICATIONS))
                        .aggregations("filtered_inner_agg", internalAgg)
                ))
        );
    }

    private Map<String, List<FacetValue>> extractFacets(SearchHits<ProductDocument> searchHits, String category) {
        final Map<String, List<FacetValue>> facets = new HashMap<>();

        ElasticsearchAggregations agg = (ElasticsearchAggregations) searchHits.getAggregations();
        if (agg == null) return facets;

        try {
            extractFilteredTermsFacet(agg, FIELD_BRAND, facets);
            extractFilteredTermsFacet(agg, FIELD_CATEGORY, facets);

            attributeSchemaValidator.getAttributeDefinitions(category).values().stream()
                    .filter(AttributeDefinition::isFacetable)
                    .forEach(def -> extractNestedFilteredFacetStrategy(agg, def.getName(), facets));

            Map<String, List<FacetValue>> finalFacets = facets;
            if (!StringUtils.hasText(category)) {
                // Si el usuario esta buscando "globalmente" sin ninguna categoria,
                // vamos a calcular que especificaciones dinamicas son las mas repetidas y le devolvemos un top 5
                List<Map.Entry<String, List<FacetValue>>> sortedDynamicFacets = facets.entrySet().stream()
                        .filter(e -> !e.getKey().equals(FIELD_BRAND) && !e.getKey().equals(FIELD_CATEGORY))
                        .sorted((e1, e2) -> {
                            long total1 = e1.getValue().stream().mapToLong(FacetValue::getCount).sum();
                            long total2 = e2.getValue().stream().mapToLong(FacetValue::getCount).sum();
                            return Long.compare(total2, total1);
                        })
                        .limit(5)
                        .toList();

                Map<String, List<FacetValue>> limitedFacets = new HashMap<>();
                if (facets.containsKey(FIELD_BRAND)) limitedFacets.put(FIELD_BRAND, facets.get(FIELD_BRAND));
                if (facets.containsKey(FIELD_CATEGORY)) limitedFacets.put(FIELD_CATEGORY, facets.get(FIELD_CATEGORY));

                for (Map.Entry<String, List<FacetValue>> entry : sortedDynamicFacets) {
                    limitedFacets.put(entry.getKey(), entry.getValue());
                }
                finalFacets = limitedFacets;
            }

            log.info("Extracted facets: {}", finalFacets);
            return finalFacets;
        } catch (Exception e) {
            log.warn("Failed to extract facets: {}", e.getMessage());
        }

        return facets;
    }

    private void extractFilteredTermsFacet(ElasticsearchAggregations agg, String facetName, Map<String,
            List<FacetValue>> facets) {
        try {
            Aggregate filterAggregate = agg.get(facetName) != null ? agg.get(facetName).aggregation().getAggregate()
                    : null;
            if (filterAggregate == null || !filterAggregate.isFilter()) return;

            Aggregate innerAgg = filterAggregate.filter().aggregations().get("filtered_terms");

            if (innerAgg != null) {
                List<FacetValue> values = extractBucketValues(innerAgg);
                if (!values.isEmpty()) {
                    facets.put(facetName, values);
                }
            }
        } catch (Exception e) {
            log.debug("Failed to extract terms facet '{}': {}", facetName, e.getMessage());
        }
    }

    private void extractNestedFilteredFacetStrategy(ElasticsearchAggregations agg, String facetName, Map<String,
            List<FacetValue>> facets) {
        try {
            Aggregate filterAggregate = agg.get(facetName) != null ? agg.get(facetName).aggregation().getAggregate()
                    : null;
            if (filterAggregate == null || !filterAggregate.isFilter()) return;

            Aggregate nestedAgg = filterAggregate.filter().aggregations().get("nested_aggs");
            if (nestedAgg == null || !nestedAgg.isNested()) return;

            Aggregate innerAgg = nestedAgg.nested().aggregations().get("filtered_inner_agg");
            if (innerAgg == null) return;

            List<FacetValue> values;
            if (innerAgg.isSampler()) {
                Aggregate sampledTerms = innerAgg.sampler().aggregations().get("sampled_terms");
                values = sampledTerms != null ? extractBucketValues(sampledTerms) : Collections.emptyList();
            } else {
                values = extractBucketValues(innerAgg);
            }

            if (!values.isEmpty()) {
                facets.put(facetName, values);
            }
        } catch (Exception e) {
            log.debug("Failed to extract nested strategy facet '{}': {}", facetName, e.getMessage());
        }
    }

    private List<FacetValue> extractBucketValues(Aggregate aggregate) {
        // obtenemos los buckets mapeando dinamicamente segun la clase que trajo elasticsearch
        Stream<FacetValue> facetStream = switch (aggregate._kind()) {
            case Sterms -> aggregate.sterms().buckets().array().stream()
                    .map(b -> new FacetValue(b.key().stringValue(), b.docCount()));
            case Lterms -> aggregate.lterms().buckets().array().stream()
                    .map(b -> new FacetValue(String.valueOf(b.key()), b.docCount()));
            case Dterms -> aggregate.dterms().buckets().array().stream()
                    .map(b -> new FacetValue(String.valueOf(b.key()), b.docCount()));
            case Sigsterms -> aggregate.sigsterms().buckets().array().stream()
                    .map(b -> new FacetValue(b.key(), b.docCount()));
            default -> null;
        };

        return facetStream != null
                ? facetStream.filter(fv -> fv.getCount() > 0).toList()
                : Collections.emptyList();
    }

    private BigDecimal extractMinPrice(SearchHits<ProductDocument> searchHits) {
        return extractPriceStatsValue(searchHits, AGG_MIN_PRICE);
    }

    private BigDecimal extractMaxPrice(SearchHits<ProductDocument> searchHits) {
        return extractPriceStatsValue(searchHits, AGG_MAX_PRICE);
    }

    private BigDecimal extractPriceStatsValue(SearchHits<ProductDocument> searchHits, String statName) {
        try {
            ElasticsearchAggregations aggs = (ElasticsearchAggregations) searchHits.getAggregations();
            Aggregate statsAgg = aggs.get("price_stats").aggregation().getAggregate();

            Double value = statName.equals(AGG_MIN_PRICE)
                    ? statsAgg.filter().aggregations().get(AGG_MIN_PRICE).min().value()
                    : statsAgg.filter().aggregations().get(AGG_MAX_PRICE).max().value();

            if (value != null && !value.isNaN() && !value.isInfinite()) {
                return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
            }
            return null;
        } catch (Exception e) {
            log.debug("Failed to extract price stat '{}': {}", statName, e.getMessage());
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
            log.debug("todos loa atributos son invalidos: {}", filters.getCategory());
        }
    }

    private boolean isValidPrice(BigDecimal price) {
        return price != null && price.compareTo(BigDecimal.ZERO) > 0;
    }
}
