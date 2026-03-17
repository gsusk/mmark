package org.adso.minimarket.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class CreatePaymentResponse {
    private String paymentReference;
    private String currency;
    private BigDecimal amount;
    private String status;
    private UUID orderId;
}