package org.adso.minimarket.repository.jpa;

import org.adso.minimarket.models.cart.Cart;
import org.adso.minimarket.models.cart.CartStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CartRepository extends JpaRepository<Cart, Long> {
    @Query("""
                SELECT c FROM Cart c
                LEFT JOIN FETCH c.cartItems ci
                LEFT JOIN FETCH ci.product
                WHERE c.guestId = :guestId AND c.status = :status
            """)
    Optional<Cart> findCartWithProductsByGuestIdAndStatus(UUID guestId, CartStatus status);

    @Query("""
                SELECT c FROM Cart c
                LEFT JOIN FETCH c.cartItems ci
                LEFT JOIN FETCH ci.product
                WHERE c.user = :userId AND c.status = :status
            """)
    Optional<Cart> findCartWithProductsByUserIdAndStatus(Long userId, CartStatus status);

    Optional<Cart> findCartByGuestIdAndStatus(UUID guestId, CartStatus status);

    Optional<Cart> findCartByUserIdAndStatus(Long userId, CartStatus status);

    List<Cart> findCartsByUserId(Long userId);

    @EntityGraph(attributePaths = {"cartItems", "cartItems.product"})
    Optional<Cart> findWithItemsByUserIdAndStatus(Long userId, CartStatus cartStatus);

    @EntityGraph(attributePaths = {"cartItems", "cartItems.product"})
    Optional<Cart> findWithItemsByGuestIdAndStatus(UUID guestId, CartStatus cartStatus);
}
