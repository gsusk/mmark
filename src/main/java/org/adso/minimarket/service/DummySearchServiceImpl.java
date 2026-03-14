package org.adso.minimarket.service;

import lombok.extern.slf4j.Slf4j;
import org.adso.minimarket.dto.SearchFilters;
import org.adso.minimarket.dto.SearchResult;
import org.adso.minimarket.models.document.ProductDocument;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Slf4j
public class DummySearchServiceImpl implements SearchService {

    @Override
    public void saveIndex(ProductDocument product) {
        log.info("Search disabled: skipping indexing for product {}", product.getId());
    }

    @Override
    public SearchResult searchWithFilters(SearchFilters filters, String query) {
        log.info("Search disabled: returning empty search results");
        return SearchResult.builder()
                .products(Collections.emptyList())
                .total(0L)
                .build();
    }
}
