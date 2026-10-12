package org.adso.minimarket.service;

import org.adso.minimarket.dto.CreatePaymentResponse;
import org.adso.minimarket.dto.CreatePaymentRequest;

import java.util.UUID;

public interface PaymentService {
    CreatePaymentResponse createPayment(CreatePaymentRequest request);
    CreatePaymentResponse getPaymentByOrderId(UUID orderId, Long userId);
    CreatePaymentResponse confirmPayment(UUID orderId, Long userId, String paymentMethodId);
    void processWebhook(String payload, String sigHeader);
}
