package org.adso.minimarket.service;

import org.adso.minimarket.exception.InventoryAdjustmentException;
import org.adso.minimarket.exception.NotFoundException;
import org.adso.minimarket.models.inventory.InventoryTransaction;
import org.adso.minimarket.models.inventory.TransactionType;
import org.adso.minimarket.models.product.Product;
import org.adso.minimarket.repository.jpa.InventoryTransactionRepository;
import org.adso.minimarket.repository.jpa.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InventoryServiceImpl implements InventoryService {

    private final ProductRepository productRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;

    public InventoryServiceImpl(ProductRepository productRepository, InventoryTransactionRepository inventoryTransactionRepository) {
        this.productRepository = productRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
    }

    @Override
    @Transactional
    public void adjustStock(Long productId, int quantity, TransactionType type, String reason) {
        Product product = productRepository.findForUpdateById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found"));

        int newStock = product.getStock() + quantity;
        if (newStock < 0) {
            throw new InventoryAdjustmentException("Insufficient stock for product: " + product.getName() + ". Current stock: " + product.getStock() + ", requested adjustment: " + quantity);
        }

        product.setStock(newStock);
        productRepository.save(product);

        InventoryTransaction transaction = new InventoryTransaction(product, quantity, type, reason);
        inventoryTransactionRepository.save(transaction);
    }

    @Override
    @Transactional
    public void logTransaction(Product product, int quantity, TransactionType type, String reason) {
        InventoryTransaction transaction = new InventoryTransaction(product, quantity, type, reason);
        inventoryTransactionRepository.save(transaction);
    }

    @Override
    public List<InventoryTransaction> getStockHistory(Long productId) {
        return inventoryTransactionRepository.findByProductIdOrderByCreatedAtDesc(productId);
    }
}
