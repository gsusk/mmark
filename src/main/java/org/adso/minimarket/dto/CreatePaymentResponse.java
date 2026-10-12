package org.adso.minimarket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreatePaymentResponse {
    private String paymentReference;
    private String clientSecret;
    private String publishableKey;
    private String currency;
    private BigDecimal amount;
    private String status;
    private UUID orderId;
}