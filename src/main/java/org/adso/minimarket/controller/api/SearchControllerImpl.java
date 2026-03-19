package org.adso.minimarket.controller.api;

import org.adso.minimarket.dto.SearchFilters;
import org.adso.minimarket.dto.SearchResult;
import org.adso.minimarket.service.SearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Controller
public class SearchControllerImpl implements SearchController {
    private static final Logger log = LoggerFactory.getLogger(SearchControllerImpl.class);
    private final SearchService searchService;

    public SearchControllerImpl(SearchService searchService) {
        this.searchService = searchService;
    }

    @Override
    @GetMapping("/search")
    public ResponseEntity<?> searchProducts(@RequestParam Map<String, String> allParams) {
        String query = allParams.getOrDefault("q", "");
        String category = allParams.get("category");
        String brand = allParams.get("brand");
        String minValue = allParams.get("min");
        String maxValue = allParams.get("max");

        BigDecimal min = parseSafely(minValue).orElse(null);
        BigDecimal max = parseSafely(maxValue).orElse(null);

        Map<String, String> attributes = new HashMap<>(allParams);
        attributes.remove("q");
        attributes.remove("category");
        attributes.remove("brand");
        attributes.remove("min");
        attributes.remove("max");

        SearchResult productDocuments = searchService.searchWithFilters(
                new SearchFilters(
                        category,
                        brand,
                        max,
                        min,
                        attributes
                ),
                query
        );
        return ResponseEntity.ok(productDocuments);
    }

    private Optional<BigDecimal> parseSafely(String value) {
        try {
            BigDecimal result = new BigDecimal(value);
            return result.compareTo(BigDecimal.ZERO) >= 0 ? Optional.of(result) : Optional.empty();
        } catch (NumberFormatException | NullPointerException e) {
            log.debug("Invalid numeric filter value ignored: {}", value);
            return Optional.empty();
        }
    }
}
