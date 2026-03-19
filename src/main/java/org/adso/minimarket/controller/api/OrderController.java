package org.adso.minimarket.controller.api;

import org.adso.minimarket.config.UserPrincipal;
import org.adso.minimarket.dto.OrderDetails;
import org.adso.minimarket.dto.OrderSummary;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;

public interface OrderController {
    ResponseEntity<OrderDetails> placeOrder(@AuthenticationPrincipal UserPrincipal principal);

    ResponseEntity<OrderSummary> getOrderDetails(UserPrincipal principal, String orderId);

    ResponseEntity<List<OrderSummary>> getMyOrders(UserPrincipal principal);
}
