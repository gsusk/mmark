package org.adso.minimarket.controller.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.adso.minimarket.constant.ProductRoutes;
import org.adso.minimarket.dto.CreateProductRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public interface ProductController {
    ResponseEntity<?> create(@Valid @RequestBody CreateProductRequest productRequest);

    ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody CreateProductRequest productRequest);

    ResponseEntity<?> delete(@PathVariable Long id);

    ResponseEntity<?> getById(@PathVariable @Min(1) @Valid Long id);

    ResponseEntity<?> getFeatured();
}
