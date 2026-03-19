package org.adso.minimarket.controller.api;

import jakarta.validation.Valid;
import org.adso.minimarket.config.UserPrincipal;
import org.adso.minimarket.dto.CheckoutRequest;
import org.adso.minimarket.dto.CreatePaymentResponse;
import org.adso.minimarket.service.CheckoutService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

// Extender el servicio apra el proyecto
// completo usando los status pendiente si tenemos que sincronizar
// con servicios como striipe.
@RestController
public class CheckoutControllerImpl implements CheckoutController {
    private final CheckoutService checkoutService;

    public CheckoutControllerImpl(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @Override
    @PostMapping("/checkout/initialize")
    public ResponseEntity<CreatePaymentResponse> processCheckout(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody CheckoutRequest body) {

        return ResponseEntity.ok(checkoutService.processCheckout(userPrincipal.getUser(), body));
    }
}
