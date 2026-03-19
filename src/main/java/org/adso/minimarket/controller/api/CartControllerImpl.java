package org.adso.minimarket.controller.api;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Min;
import org.adso.minimarket.config.UserPrincipal;
import org.adso.minimarket.dto.AddCartItemRequest;
import org.adso.minimarket.dto.ShoppingCart;
import org.adso.minimarket.dto.UpdateQuantityRequest;
import org.adso.minimarket.service.CartService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.UUID;

@RestController
public class CartControllerImpl implements CartController {
    private final CartService cartService;

    public CartControllerImpl(CartService cartService) {
        this.cartService = cartService;
    }

    @Override
    @GetMapping("/cart")
    public ResponseEntity<ShoppingCart> getCart(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @CookieValue(value = "CGUESTID", required = false) UUID guestId) {

        Long userId = getUserIdFromPrincipal(userPrincipal);

        if (userId == null && guestId == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(cartService.getShoppingCart(userId, guestId));
    }

    @Override
    @PostMapping("/cart/items")
    public ResponseEntity<ShoppingCart> addItem(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody AddCartItemRequest body,
            @CookieValue(value = "CGUESTID", required = false) UUID guestId,
            HttpServletResponse response) {

        Long userId = getUserIdFromPrincipal(userPrincipal);
        if (userId == null && guestId == null) {
            guestId = UUID.randomUUID();
        }
        ShoppingCart cart = cartService.addItemToCart(userId, guestId, body);
        if (userId == null) {
            setGuestCookie(guestId, response);
        }
        return ResponseEntity.ok(cart);
    }

    @Override
    @DeleteMapping("/cart/items/{itemId}")
    public ResponseEntity<ShoppingCart> deleteItem(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @CookieValue(value = "CGUESTID", required = false) UUID guestId,
            @PathVariable("itemId") @Min(1) Long productId) {

        Long userId = getUserIdFromPrincipal(userPrincipal);
        if (userId == null && guestId == null) {
            return ResponseEntity.badRequest().build();
        }

        ShoppingCart cart = cartService.removeItemFromCart(userId, guestId, productId);

        return ResponseEntity.ok(cart);
    }

    @Override
    @PutMapping("/cart/items/{itemId}")
    public ResponseEntity<ShoppingCart> updateItemQuantity(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                           @RequestBody UpdateQuantityRequest body,
                                                           @CookieValue(value = "CGUESTID") UUID guestId,
                                                           @PathVariable("itemId") Long productId) {
        Long userId = getUserIdFromPrincipal(userPrincipal);
        if (userId == null && guestId == null) {
            return ResponseEntity.notFound().build();
        }
        ShoppingCart cart = cartService.updateItemQuantity(userId, guestId, productId, body.getQuantity());
        return ResponseEntity.ok(cart);
    }

    private Long getUserIdFromPrincipal(UserPrincipal userPrincipal) {
        return userPrincipal == null ? null : userPrincipal.getId();
    }

    private void setGuestCookie(UUID guestId, HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie
                .from("CGUESTID", guestId.toString())
                .maxAge(Duration.ofDays(10))
                .path("/")
                .httpOnly(true)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
