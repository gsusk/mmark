package org.adso.minimarket.service;

import org.adso.minimarket.dto.CheckoutRequest;
import org.adso.minimarket.dto.OrderDetails;
import org.adso.minimarket.dto.OrderSummary;
import org.adso.minimarket.models.user.User;

import java.util.List;
import java.util.UUID;

public interface OrderService {
    OrderDetails placeOrder(User user, CheckoutRequest checkoutRequest);

    OrderSummary getOrderById(UUID orderId, Long userId);

    List<OrderSummary> getUserOrders(Long userId);
}
