package org.adso.minimarket.unit.api;

import org.springframework.http.ResponseEntity;

import java.util.Map;

public interface SearchController {
     ResponseEntity<?> searchProducts(Map<String, String> allParams);
}
