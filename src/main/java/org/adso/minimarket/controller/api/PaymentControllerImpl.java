package org.adso.minimarket.controller.api;

import org.adso.minimarket.config.UserPrincipal;
import org.adso.minimarket.dto.CreatePaymentResponse;
import org.adso.minimarket.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

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
}
