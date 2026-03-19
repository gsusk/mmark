package org.adso.minimarket.controller.api;

import org.springframework.http.ResponseEntity;

import java.util.Map;

public interface SearchController {
     ResponseEntity<?> searchProducts(Map<String, String> allParams);
}
