package org.adso.minimarket.models.cart;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.adso.minimarket.models.user.User;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Cart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private CartStatus status = CartStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_cart_user"))
    private User user;

    private UUID guestId;

    @Getter
    @OneToMany(mappedBy = "cart", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<CartItem> cartItems = new HashSet<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    //usuario
    public Cart(User user) {
        this.user = user;
        this.guestId = null;
        this.cartItems = new HashSet<>();
    }

    //invitado
    public Cart(UUID guestId) {
        this.user = null;
        this.guestId = guestId;
        this.cartItems = new HashSet<>();
    }

    public void setStatus(CartStatus cartStatus) {
        this.status = cartStatus;
    }

    public Long getId() {
        return this.id;
    }

    public CartStatus getStatus() {
        return status;
    }


    public BigDecimal getTotal() {
        return cartItems == null ? BigDecimal.ZERO :
                cartItems.stream()
                        .map((item) -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
                        )
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .setScale(2, RoundingMode.HALF_UP);
    }

    public int getSize() {
        return cartItems.size();
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setGuestId(UUID id) {
        this.guestId = id;
    }
}
