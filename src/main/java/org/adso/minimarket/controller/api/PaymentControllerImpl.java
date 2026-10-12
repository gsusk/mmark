package org.adso.minimarket.controller.api;

import org.adso.minimarket.config.UserPrincipal;
import org.adso.minimarket.dto.ConfirmPaymentRequest;
import org.adso.minimarket.dto.CreatePaymentResponse;
import org.adso.minimarket.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class PaymentControllerImpl implements PaymentController {

    private final PaymentService paymentService;

    public PaymentControllerImpl(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Override
    @GetMapping("/orders/{order_id}/payment")
    public ResponseEntity<CreatePaymentResponse> getPaymentForOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("order_id") String orderId) {

        CreatePaymentResponse payment = paymentService.getPaymentByOrderId(
                UUID.fromString(orderId),
                principal.getId()
        );
        return ResponseEntity.ok(payment);
    }

    @Override
    @PostMapping("/orders/{order_id}/confirm-payment")
    public ResponseEntity<CreatePaymentResponse> confirmPaymentForOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("order_id") String orderId,
            @RequestBody(required = false) ConfirmPaymentRequest request) {

        String paymentMethodId = request != null ? request.getPaymentMethodId() : null;
        CreatePaymentResponse response = paymentService.confirmPayment(
                UUID.fromString(orderId),
                principal.getId(),
                paymentMethodId
        );
        return ResponseEntity.ok(response);
    }

    @Override
    @PostMapping("/api/payments/webhook")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String sigHeader) {

        paymentService.processWebhook(payload, sigHeader);
        return ResponseEntity.ok("Webhook procesado exitosamente");
    }
}
