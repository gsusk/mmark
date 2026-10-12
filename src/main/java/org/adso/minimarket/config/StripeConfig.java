package org.adso.minimarket.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Getter
@Configuration
public class StripeConfig {

    @Value("${stripe.secret-key:}")
    private String secretKey;

    @Value("${stripe.publishable-key:}")
    private String publishableKey;

    @Value("${stripe.webhook-secret:}")
    private String webhookSecret;

    @PostConstruct
    public void init() {
        if (isConfigured()) {
            Stripe.apiKey = secretKey.trim();
            log.info("[StripeConfig] Stripe inicializado exitosamente con clave secreta configurada.");
        } else {
            log.warn("[StripeConfig] No se detectó clave secreta de Stripe (STRIPE_SECRET_KEY). Operando en modo simulado/fallback.");
        }
    }

    public boolean isConfigured() {
        return secretKey != null && !secretKey.isBlank() && !secretKey.startsWith("dummy");
    }
}
