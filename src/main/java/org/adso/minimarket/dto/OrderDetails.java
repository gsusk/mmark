package org.adso.minimarket.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetails {
    @JsonProperty("orderId")
    private UUID id;
    private String email;
    private Long userId;
    private String status;
    private BigDecimal total;
    private List<OrderItemSummary> items;
    private LocalDateTime createdAt;
}
