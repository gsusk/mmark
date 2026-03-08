package org.adso.minimarket.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShoppingCartItem {
    private Long productId;
    private String name;
    private String unitPrice;
    private int quantity;
}
