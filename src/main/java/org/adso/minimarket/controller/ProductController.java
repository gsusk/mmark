package org.adso.minimarket.unit.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.adso.minimarket.dto.CreateProductRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public interface ProductController {
    ResponseEntity<?> create(@Valid @RequestBody CreateProductRequest productRequest);

    ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody CreateProductRequest productRequest);

    ResponseEntity<?> delete(@PathVariable Long id);

    ResponseEntity<?> getById(@PathVariable @Min(1) @Valid Long id);

    ResponseEntity<?> getFeatured();
}
