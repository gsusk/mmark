package org.adso.minimarket.unit.api;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.adso.minimarket.config.UserPrincipal;
import org.adso.minimarket.dto.AddCartItemRequest;
import org.adso.minimarket.dto.ShoppingCart;
import org.adso.minimarket.dto.UpdateQuantityRequest;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

public interface CartController {
    ResponseEntity<ShoppingCart> getCart(UserPrincipal userPrincipal,
                                         UUID guestId);

    ResponseEntity<ShoppingCart> addItem(UserPrincipal userPrincipal,
                                         @Valid AddCartItemRequest body,
                                         UUID guestId,
                                         HttpServletResponse response);

    ResponseEntity<ShoppingCart> deleteItem(UserPrincipal userPrincipal,
                                            UUID guestId,
                                            @Min(1) Long productId);

    ResponseEntity<ShoppingCart> updateItemQuantity(UserPrincipal userPrincipal,
                                                    @Valid UpdateQuantityRequest body,
                                                    UUID guestId,
                                                    @Min(1) Long productId);
}
