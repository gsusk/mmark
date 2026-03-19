package org.adso.minimarket.unit.api;

import lombok.extern.slf4j.Slf4j;
import org.adso.minimarket.config.UserPrincipal;
import org.adso.minimarket.dto.OrderDetails;
import org.adso.minimarket.dto.OrderSummary;
import org.adso.minimarket.exception.BadRequestException;
import org.adso.minimarket.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
public class OrderControllerImpl implements OrderController {
    private final OrderService orderService;

    public OrderControllerImpl(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/orders")
    @Override
    public ResponseEntity<OrderDetails> placeOrder(
            @AuthenticationPrincipal UserPrincipal principal) {

        OrderDetails orderDetails = orderService.placeOrder(principal.getUser(), null);
        return ResponseEntity.ok(orderDetails);
    }

    @Override
    @GetMapping("/orders/{order_id}")
    public ResponseEntity<OrderSummary> getOrderDetails(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("order_id") String orderId) {
        UUID parsedOrderId;
        try {
            parsedOrderId = UUID.fromString(orderId);
        } catch (Exception e) {
            log.error("Error parsing order id: {}", e.getMessage());
            throw new BadRequestException("orderId invalida o malformada");
        }

        OrderSummary summary = orderService.getOrderById(parsedOrderId, principal.getId());
        return ResponseEntity.ok(summary);
    }

    @Override
    @GetMapping("/orders")
    public ResponseEntity<List<OrderSummary>> getMyOrders(
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(orderService.getUserOrders(principal.getId()));
    }
}
