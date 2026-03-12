package org.adso.minimarket.models.product;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.adso.minimarket.models.Category;
import org.adso.minimarket.models.cart.CartItem;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SourceType;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.*;

@Getter
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false, length = 300)
    private String slug;

    private String description;

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer stock;

    @Column(nullable = true)
    private String brand;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attributes", columnDefinition = "json")
    private Map<String, Object> attributes = new HashMap<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<Image> images = new ArrayList<>();

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private Set<CartItem> cartItems = new HashSet<>();

    @CreationTimestamp(source = SourceType.DB)
    private LocalDateTime createdAt;

    @UpdateTimestamp(source = SourceType.DB)
    private LocalDateTime updatedAt;

    public Product(String name, String description, BigDecimal price, Integer stock, Category category, String brand,
                   Map<String, Object> attributes) {
        this.name = name;
        this.slug = generateSlug(name);
        this.description = description;
        this.price = normalizePrice(price);
        this.category = category;
        if (stock == null || stock < 0) {
            throw new IllegalArgumentException("Invalid stock argument");
        }
        this.stock = stock;
        this.brand = brand;
        this.attributes = attributes;
    }

    BigDecimal normalizePrice(BigDecimal price) {
        if (price == null) {
            throw new IllegalArgumentException("Price cannot be null");
        }
        if (price.signum() < 0) {
            throw new IllegalArgumentException("Price cannot be negative");
        }
        return price.setScale(2, RoundingMode.HALF_UP);
    }

    public void setPrice(BigDecimal price) {
        this.price = normalizePrice(price);
    }

    public void setStock(Integer stock) {
        if (stock == null || stock < 0) {
            throw new IllegalArgumentException("Invalid stock value");
        }
        this.stock = stock;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Product other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public void setName(String name) {
        this.name = name;
        this.slug = generateSlug(name);
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public static String generateSlug(String name) {
        if (name == null) return null;
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD);
        String stripped = normalized.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        return stripped.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("/", "-")
                .replaceAll("[\\s-]+", "-");
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public void setImages(List<Image> images) {
        this.images = images;
    }

    public void setCartItems(Set<CartItem> cartItems) {
        this.cartItems = cartItems;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
