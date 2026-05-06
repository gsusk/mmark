package org.adso.minimarket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.adso.minimarket.models.inventory.TransactionType;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InventoryTransactionDTO {
    private Long id;
    private Integer quantity;
    private TransactionType type;
    private String reason;
    private LocalDateTime createdAt;
    private Long productId;
    private String productName;
}
