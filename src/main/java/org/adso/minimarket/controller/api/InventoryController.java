package org.adso.minimarket.controller.api;

import jakarta.validation.Valid;
import org.adso.minimarket.dto.InventoryAdjustmentRequest;
import org.adso.minimarket.models.inventory.InventoryTransaction;
import org.adso.minimarket.service.InventoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@PreAuthorize("hasRole('ADMIN')")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/{productId}/adjust")
    public ResponseEntity<Void> adjustStock(@PathVariable Long productId,
                                            @RequestBody @Valid InventoryAdjustmentRequest request) {
        inventoryService.adjustStock(productId, request.getQuantity(), request.getType(), request.getReason());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{productId}/history")
    public ResponseEntity<List<InventoryTransaction>> getStockHistory(@PathVariable Long productId) {
        return ResponseEntity.ok(inventoryService.getStockHistory(productId));
    }
}
