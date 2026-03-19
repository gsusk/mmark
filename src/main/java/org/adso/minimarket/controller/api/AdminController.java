package org.adso.minimarket.controller.api;

import org.adso.minimarket.dto.OrderSummary;
import org.adso.minimarket.mappers.OrderMapper;
import org.adso.minimarket.repository.jpa.OrderRepository;
import org.adso.minimarket.service.InventoryService;
import org.adso.minimarket.models.inventory.TransactionType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final InventoryService inventoryService;

    public AdminController(OrderRepository orderRepository,
                           OrderMapper orderMapper,
                           InventoryService inventoryService) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
        this.inventoryService = inventoryService;
    }

    @GetMapping("/orders")
    public ResponseEntity<List<OrderSummary>> getAllOrders() {
        List<OrderSummary> orders = orderMapper.toOrderSummaryDtoList(
                orderRepository.findAllByOrderByCreatedAtDesc()
        );
        return ResponseEntity.ok(orders);
    }

    @PostMapping("/inventory/{productId}/adjust")
    public ResponseEntity<?> adjustStock(
            @PathVariable Long productId,
            @RequestBody Map<String, Object> body) {
        int quantity = (int) body.get("quantity");
        String reason = body.getOrDefault("reason", "Ajuste manual").toString();
        TransactionType type = quantity >= 0 ? TransactionType.RESTOCK : TransactionType.RETURN;
        inventoryService.adjustStock(productId, quantity, type, reason);
        return ResponseEntity.ok().build();
    }
}
