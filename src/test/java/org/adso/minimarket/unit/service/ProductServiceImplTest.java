package org.adso.minimarket.unit.service;

import org.adso.minimarket.dto.CreateProductRequest;
import org.adso.minimarket.dto.DetailedProduct;
import org.adso.minimarket.exception.NotFoundException;
import org.adso.minimarket.mappers.ProductMapper;
import org.adso.minimarket.models.Category;
import org.adso.minimarket.models.product.Product;
import org.adso.minimarket.repository.jpa.ProductRepository;
import org.adso.minimarket.service.CategoryService;
import org.adso.minimarket.service.InventoryService;
import org.adso.minimarket.service.ProductServiceImpl;
import org.adso.minimarket.service.SearchService;
import org.adso.minimarket.validation.ProductAttributeValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProductServiceImplTest {

    @InjectMocks
    private ProductServiceImpl productService;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryService categoryService;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private SearchService searchService;

    @Mock
    private ProductAttributeValidator attributeValidator;

    @Mock
    private InventoryService inventoryService;

    private Category buildCategory(Long id, String name) {
        Category c = new Category(id, name, List.of(), null);
        return c;
    }

    private CreateProductRequest buildRequest(String name, BigDecimal price, Long catId, int stock) {
        CreateProductRequest req = new CreateProductRequest();
        req.setName(name);
        req.setDescription("desc");
        req.setBrand("TestBrand");
        req.setPrice(price);
        req.setCategoryId(catId);
        req.setStock(stock);
        req.setSpecifications(new HashMap<>());
        return req;
    }

    @Test
    @DisplayName("Crear producto con solicitud válida guarda el producto y retorna el ID")
    void crearProducto_conSolicitudValida_guardaProductoYRetornaId() {
        Category category = buildCategory(1L, "Ropa");
        CreateProductRequest req = buildRequest("Camisa", new BigDecimal("50.00"), 1L, 10);

        Product savedProduct = new Product("Camisa", "desc", new BigDecimal("50.00"), 10, category, "TestBrand", new HashMap<>());
        ReflectionTestUtils.setField(savedProduct, "id", 99L);

        when(categoryService.getById(1L)).thenReturn(category);
        doNothing().when(attributeValidator).validate(any(), any());
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);
        doNothing().when(searchService).saveIndex(any());
        doNothing().when(inventoryService).logTransaction(any(), anyInt(), any(), anyString());

        Long resultId = productService.createProduct(req);

        assertEquals(99L, resultId);
        verify(categoryService).getById(1L);
        verify(productRepository).save(any(Product.class));
        verify(inventoryService).logTransaction(any(), eq(10), any(), anyString());
    }

    @Test
    @DisplayName("Crear producto con stock cero no registra transaccionn inicial")
    void crearProducto_conStockCero_noRegistraTransaccionInicial() {
        Category category = buildCategory(1L, "Ropa");
        CreateProductRequest req = buildRequest("Camisa", new BigDecimal("50.00"), 1L, 0);

        Product savedProduct = new Product("Camisa", "desc", new BigDecimal("50.00"), 0, category, "TestBrand", new HashMap<>());
        ReflectionTestUtils.setField(savedProduct, "id", 100L);

        when(categoryService.getById(1L)).thenReturn(category);
        doNothing().when(attributeValidator).validate(any(), any());
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);
        doNothing().when(searchService).saveIndex(any());

        Long resultId = productService.createProduct(req);

        assertEquals(100L, resultId);
        verifyNoInteractions(inventoryService);
    }

    @Test
    @DisplayName("Crear producto cuando no se encuentra la categoraa lanza NotFoundException")
    void crearProducto_cuandoCategoriaNoSeEncuentra_lanzaNotFoundException() {
        when(categoryService.getById(99L)).thenThrow(new NotFoundException("Category not found"));

        CreateProductRequest req = buildRequest("Camisa", new BigDecimal("50.00"), 99L, 5);

        assertThrows(NotFoundException.class, () -> productService.createProduct(req));
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Obtener por id cuando el producto existe retorna el producto")
    void obtenerPorId_cuandoProductoExiste_retornaProducto() {
        Category category = buildCategory(1L, "Ropa");
        Product product = new Product("Camisa", "desc", new BigDecimal("50.00"), 5, category, "Brand", new HashMap<>());
        ReflectionTestUtils.setField(product, "id", 1L);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        Product result = productService.getById(1L);

        assertEquals("Camisa", result.getName());
        verify(productRepository).findById(1L);
    }

    @Test
    @DisplayName("Obtener por id cuando no se encuentra lanza NotFoundException")
    void obtenerPorId_cuandoNoSeEncuentra_lanzaNotFoundException() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> productService.getById(999L));
    }

    @Test
    @DisplayName("obtener producto detallado por id cuando se encuentra retorna DTO mapeado")
    void obtenerProductoDetalladoPorId_cuandoSeEncuentra_retornaDtoMapeado() {
        Category category = buildCategory(1L, "Ropa");
        Product product = new Product("Camisa", "desc", new BigDecimal("50.00"), 5, category, "Brand", new HashMap<>());
        ReflectionTestUtils.setField(product, "id", 1L);

        DetailedProduct dto = DetailedProduct.builder()
                .id(1L).name("Camisa").price("50.00").stock(5).build();

        when(productRepository.findDetailedById(1L)).thenReturn(Optional.of(product));
        when(productMapper.toDto(any(Product.class))).thenReturn(dto);

        DetailedProduct result = productService.getDetailedProductById(1L);

        assertEquals("Camisa", result.getName());
        assertEquals(1L, result.getId());
        verify(productMapper).toDto(any(Product.class));
    }

    @Test
    @DisplayName("Obtener producto detallado por id cuando no se encuentra lanza NotFoundException")
    void obtenerProductoDetalladoPorId_cuandoNoSeEncuentra_lanzaNotFoundException() {
        when(productRepository.findDetailedById(999L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> productService.getDetailedProductById(999L));
        verifyNoInteractions(productMapper);
    }

    @Test
    @DisplayName("Eliminar producto llama al repositorio para borrar por id")
    void eliminarProducto_llamaARepositorioBorrarPorId() {
        doNothing().when(productRepository).deleteById(1L);

        productService.deleteProduct(1L);

        verify(productRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Obtener productos destacados retorna hasta ocho productos")
    void obtenerProductosDestacados_retornaHastaOchoProductos() {
        Category category = buildCategory(1L, "Ropa");
        Product p1 = new Product("P1", "d", new BigDecimal("10"), 1, category, "B", new HashMap<>());
        Product p2 = new Product("P2", "d", new BigDecimal("20"), 2, category, "B", new HashMap<>());
        DetailedProduct dp1 = DetailedProduct.builder().id(1L).name("P1").build();
        DetailedProduct dp2 = DetailedProduct.builder().id(2L).name("P2").build();

        when(productRepository.findTop8ByOrderByCreatedAtDesc()).thenReturn(List.of(p1, p2));
        when(productMapper.toDto(p1)).thenReturn(dp1);
        when(productMapper.toDto(p2)).thenReturn(dp2);

        List<DetailedProduct> result = productService.getFeaturedProducts();

        assertEquals(2, result.size());
        verify(productRepository).findTop8ByOrderByCreatedAtDesc();
    }

    @Test
    @DisplayName("Actualizar producto con datos validos actualiza y guarda")
    void actualizarProducto_conDatosValidos_actualizaYGuarda() {
        Category category = buildCategory(1L, "Ropa");
        Product existing = new Product("Viejo", "desc", new BigDecimal("30.00"), 5, category, "OldBrand", new HashMap<>());
        ReflectionTestUtils.setField(existing, "id", 1L);

        CreateProductRequest req = buildRequest("Nuevo", new BigDecimal("60.00"), 1L, 5);

        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryService.getById(1L)).thenReturn(category);
        doNothing().when(attributeValidator).validate(any(), any());
        when(productRepository.save(any(Product.class))).thenReturn(existing);
        doNothing().when(searchService).saveIndex(any());

        productService.updateProduct(1L, req);

        assertEquals("Nuevo", existing.getName());
        assertEquals(new BigDecimal("60.00").setScale(2, java.math.RoundingMode.HALF_UP), existing.getPrice());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Actualizar producto cuando no se encuentra lanza NotFoundException")
    void actualizarProducto_cuandoNoSeEncuentra_lanzaNotFoundException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        CreateProductRequest req = buildRequest("Nuevo", new BigDecimal("50.00"), 1L, 5);

        assertThrows(NotFoundException.class, () -> productService.updateProduct(99L, req));
        verify(productRepository, never()).save(any());
    }
}
