package org.adso.minimarket.service;

import lombok.extern.slf4j.Slf4j;
import org.adso.minimarket.models.inventory.TransactionType;
import org.adso.minimarket.models.order.Order;
import org.adso.minimarket.models.order.OrderItem;
import org.adso.minimarket.models.order.OrderStatus;
import org.adso.minimarket.repository.jpa.OrderRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class OrderExpirationService {

    static final int EXPIRATION_MINUTES = 30;

    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;

    public OrderExpirationService(OrderRepository orderRepository,
                                  InventoryService inventoryService) {
        this.orderRepository = orderRepository;
        this.inventoryService = inventoryService;
    }

    /**
     * corre cada 5 minutos y busca ordenes PENDING cuyo
     * createdAt sea anterior al corte y las cancela todas juntas
     */
    @Scheduled(fixedRate = 5 * 60 * 1000)
    @Transactional
    public void cancelExpiredOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(EXPIRATION_MINUTES);

        List<Order> expired = orderRepository.findAllByStatusAndCreatedAtBefore(
                OrderStatus.PENDING, cutoff);

        if (expired.isEmpty()) return;

        for (Order order : expired) {
            restoreStock(order);
            order.setStatus(OrderStatus.CANCELLED);
        }

        orderRepository.saveAll(expired);
        log.info("[OrderExpirationService] {} orden(es) canceladas y stock restaurado (> {} min).",
                expired.size(), EXPIRATION_MINUTES);
    }

    private void restoreStock(Order order) {
        for (OrderItem item : order.getOrderItems()) {
            Long productId = item.getProduct().getId();
            int qty = item.getQuantity();
            try {
                inventoryService.adjustStock(
                        productId,
                        qty,                         
                        TransactionType.RETURN,
                        "Orden expirada cancelada: " + order.getId()
                );
            } catch (Exception e) {
                // logeamos el error pero continuamos con lo que se pueda
                log.error("[OrderExpirationService] Error restaurando stock del producto {}: {}",
                        productId, e.getMessage());
            }
        }
    }
}
