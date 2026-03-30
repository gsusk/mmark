package org.adso.minimarket.unit.service;

import org.adso.minimarket.exception.InventoryAdjustmentException;
import org.adso.minimarket.exception.NotFoundException;
import org.adso.minimarket.models.inventory.InventoryTransaction;
import org.adso.minimarket.models.inventory.TransactionType;
import org.adso.minimarket.models.product.Product;
import org.adso.minimarket.repository.jpa.InventoryTransactionRepository;
import org.adso.minimarket.repository.jpa.ProductRepository;
import org.adso.minimarket.service.InventoryServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceImplTest {

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryTransactionRepository inventoryTransactionRepository;

    private Product buildProduct(Long id, String name, int stock) {
        Product p = new Product(name, "desc", new BigDecimal("10.00"), stock, null, null, new HashMap<>());
        ReflectionTestUtils.setField(p, "id", id);
        return p;
    }

    @Test
    void adjustStock_withValidDecrease_updatesStockAndLogsTransaction() {
        Product product = buildProduct(1L, "Camisa", 10);

        when(productRepository.findForUpdateById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(inventoryTransactionRepository.save(any(InventoryTransaction.class)))
                .thenAnswer(i -> i.getArguments()[0]);

        inventoryService.adjustStock(1L, -3, TransactionType.SALE, "Order placed");

        assertEquals(7, product.getStock());
        verify(productRepository).save(product);
        verify(inventoryTransactionRepository).save(any(InventoryTransaction.class));
    }

    @Test
    void adjustStock_withPositiveIncrease_updatesStockCorrectly() {
        Product product = buildProduct(1L, "Camisa", 5);

        when(productRepository.findForUpdateById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(inventoryTransactionRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        inventoryService.adjustStock(1L, 20, TransactionType.RESTOCK, "Restock");

        assertEquals(25, product.getStock());
    }

    @Test
    void adjustStock_whenProductNotFound_throwsNotFoundException() {
        when(productRepository.findForUpdateById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> inventoryService.adjustStock(99L, -1, TransactionType.SALE, "reason"));

        verifyNoInteractions(inventoryTransactionRepository);
    }

    @Test
    void adjustStock_whenResultNegative_throwsInventoryAdjustmentException() {
        Product product = buildProduct(1L, "Camisa", 2);

        when(productRepository.findForUpdateById(1L)).thenReturn(Optional.of(product));

        assertThrows(InventoryAdjustmentException.class,
                () -> inventoryService.adjustStock(1L, -10, TransactionType.SALE, "sale"));

        verify(productRepository, never()).save(any());
        verifyNoInteractions(inventoryTransactionRepository);
    }

    @Test
    void adjustStock_toExactZero_isAllowed() {
        Product product = buildProduct(1L, "Pantalon", 5);

        when(productRepository.findForUpdateById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(inventoryTransactionRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        assertDoesNotThrow(() -> inventoryService.adjustStock(1L, -5, TransactionType.SALE, "sold all"));

        assertEquals(0, product.getStock());
    }

    @Test
    void logTransaction_savesTransactionWithCorrectFields() {
        Product product = buildProduct(1L, "Zapatos", 20);

        when(inventoryTransactionRepository.save(any(InventoryTransaction.class)))
                .thenAnswer(i -> i.getArguments()[0]);

        inventoryService.logTransaction(product, 5, TransactionType.RESTOCK, "Initial stock");

        verify(inventoryTransactionRepository).save(any(InventoryTransaction.class));
        verifyNoInteractions(productRepository);
    }

    @Test
    void getStockHistory_returnsTransactionList() {
        Product product = buildProduct(1L, "Camisa", 10);
        InventoryTransaction t1 = new InventoryTransaction(product, -3, TransactionType.SALE, "sale1");
        InventoryTransaction t2 = new InventoryTransaction(product, 10, TransactionType.RESTOCK, "restock1");

        when(inventoryTransactionRepository.findByProductIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(t1, t2));

        List<InventoryTransaction> result = inventoryService.getStockHistory(1L);

        assertEquals(2, result.size());
        verify(inventoryTransactionRepository).findByProductIdOrderByCreatedAtDesc(1L);
    }

    @Test
    void getStockHistory_whenNoTransactions_returnsEmptyList() {
        when(inventoryTransactionRepository.findByProductIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of());

        List<InventoryTransaction> result = inventoryService.getStockHistory(1L);

        assertTrue(result.isEmpty());
    }
}
