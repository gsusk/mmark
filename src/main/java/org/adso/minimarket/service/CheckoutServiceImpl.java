package org.adso.minimarket.service;

import org.adso.minimarket.dto.CheckoutRequest;
import org.adso.minimarket.dto.CreatePaymentRequest;
import org.adso.minimarket.dto.CreatePaymentResponse;
import org.adso.minimarket.dto.OrderDetails;
import org.adso.minimarket.models.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CheckoutServiceImpl implements CheckoutService {
    private final OrderService orderService;
    private final PaymentService paymentService;

    public CheckoutServiceImpl(OrderService orderService, PaymentService paymentService) {
        this.orderService = orderService;
        this.paymentService = paymentService;
    }

    @Override
    @Transactional
    public CreatePaymentResponse processCheckout(User user, CheckoutRequest request) {
        OrderDetails order = orderService.placeOrder(user, request);

        return paymentService.createPayment(
                CreatePaymentRequest.builder()
                        .id(order.getId())
                        .email(order.getEmail())
                        .total(order.getTotal())
                        .status(order.getStatus())
                        .userId(order.getUserId())
                        .shippingFullName(request.getShippingFullName())
                        .shippingAddressLine(request.getShippingAddressLine())
                        .shippingCity(request.getShippingCity())
                        .shippingZipCode(request.getShippingZipCode())
                        .shippingCountry(request.getShippingCountry())
                        .build()
        );
    }
}
