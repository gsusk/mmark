package org.adso.minimarket.controller.api;

import org.adso.minimarket.config.UserPrincipal;
import org.adso.minimarket.dto.CreatePaymentResponse;
import org.springframework.http.ResponseEntity;

public interface PaymentController {
    ResponseEntity<CreatePaymentResponse> getPaymentForOrder(UserPrincipal principal, String orderId);
}
