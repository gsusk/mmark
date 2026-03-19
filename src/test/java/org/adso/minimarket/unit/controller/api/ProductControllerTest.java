package org.adso.minimarket.unit;

import org.adso.minimarket.constant.ProductRoutes;
import org.adso.minimarket.dto.CreateProductRequest;
import org.adso.minimarket.dto.DetailedProduct;
import org.adso.minimarket.exception.NotFoundException;
import org.adso.minimarket.service.AppUserDetailsServiceImpl;
import org.adso.minimarket.service.JwtService;
import org.adso.minimarket.service.ProductService;
import org.adso.minimarket.unit.api.ProductControllerImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@WebMvcTest(ProductControllerImpl.class)
@ExtendWith(SpringExtension.class)
public class ProductControllerTest {

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AppUserDetailsServiceImpl userDetailsService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private CreateProductRequest buildValidRequest() {
        CreateProductRequest req = new CreateProductRequest();
        req.setName("Camisa");
        req.setDescription("Descripcion");
        req.setBrand("Nike");
        req.setPrice(new java.math.BigDecimal("50.00"));
        req.setCategoryId(1L);
        req.setStock(10);
        req.setSpecifications(new HashMap<>());
        return req;
    }

    // ── POST /products ────────────────────────────────────────────────────────

    @Test
    void create_withValidRequest_returns201WithLocationHeader() throws Exception {
        when(productService.createProduct(any(CreateProductRequest.class))).thenReturn(1L);

        mockMvc.perform(
                post(ProductRoutes.CREATE_PRODUCT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildValidRequest()))
        )
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/products/1"));

        verify(productService).createProduct(any(CreateProductRequest.class));
    }

    @Test
    void create_withMissingName_returns400() throws Exception {
        CreateProductRequest req = buildValidRequest();
        req.setName("");

        mockMvc.perform(
                post(ProductRoutes.CREATE_PRODUCT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
        )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(productService);
    }

    @Test
    void create_withNullCategoryId_returns400() throws Exception {
        CreateProductRequest req = buildValidRequest();
        req.setCategoryId(null);

        mockMvc.perform(
                post(ProductRoutes.CREATE_PRODUCT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
        )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(productService);
    }

    // ── GET /products/{id} ────────────────────────────────────────────────────

    @Test
    void getById_whenFound_returns200WithProduct() throws Exception {
        DetailedProduct product = DetailedProduct.builder()
                .id(1L)
                .name("Camisa")
                .price("50.00")
                .build();

        when(productService.getDetailedProductById(1L)).thenReturn(product);

        mockMvc.perform(get("/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Camisa"))
                .andExpect(jsonPath("$.id").value(1));

        verify(productService).getDetailedProductById(1L);
    }

    @Test
    void getById_withStringId_returns400() throws Exception {
        mockMvc.perform(get("/products/abc"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(productService);
    }

    @Test
    void getById_whenNotFound_returns404() throws Exception {
        when(productService.getDetailedProductById(99L)).thenThrow(new NotFoundException("Product not found"));

        mockMvc.perform(get("/products/99"))
                .andExpect(status().isNotFound());
    }

    // ── GET /products/featured ────────────────────────────────────────────────

    @Test
    void getFeatured_returns200WithList() throws Exception {
        DetailedProduct p1 = DetailedProduct.builder().id(1L).name("P1").build();
        DetailedProduct p2 = DetailedProduct.builder().id(2L).name("P2").build();

        when(productService.getFeaturedProducts()).thenReturn(List.of(p1, p2));

        mockMvc.perform(get(ProductRoutes.GET_FEATURED_PRODUCTS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("P1"))
                .andExpect(jsonPath("$[1].name").value("P2"));
    }
}
