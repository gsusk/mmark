package org.adso.minimarket.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ProductCard(
        Long id,
        String name,
        String slug,
        String category,
        BigDecimal price,
        String brand,
        LocalDateTime createdAt,
        List<String> images
) {
}


