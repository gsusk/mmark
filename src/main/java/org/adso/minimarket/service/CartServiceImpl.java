package org.adso.minimarket.service;

import org.adso.minimarket.dto.AddCartItemRequest;
import org.adso.minimarket.dto.ShoppingCart;
import org.adso.minimarket.exception.InternalErrorException;
import org.adso.minimarket.exception.NotFoundException;
import org.adso.minimarket.exception.OrderInsufficientStockException;
import org.adso.minimarket.mappers.CartMapper;
import org.adso.minimarket.models.cart.Cart;
import org.adso.minimarket.models.cart.CartItem;
import org.adso.minimarket.models.cart.CartStatus;
import org.adso.minimarket.models.product.Product;
import org.adso.minimarket.models.user.User;
import org.adso.minimarket.repository.jpa.CartRepository;
import org.adso.minimarket.repository.jpa.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {
    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final CartMapper cartMapper;
    private final ProductService productService;

    public CartServiceImpl(CartRepository cartRepository, UserRepository userRepository, CartMapper cartMapper,
                           ProductService productService) {
        this.cartRepository = cartRepository;
        this.userRepository = userRepository;
        this.cartMapper = cartMapper;
        this.productService = productService;
    }

    @Override
    @Transactional(readOnly = true)
    public ShoppingCart getShoppingCart(Long userId, UUID guestId) {
        return cartMapper.toDto(getCart(userId, guestId));
    }

    @Override
    public Cart getCart(Long userId, UUID guestId) {
        Optional<Cart> cart = Optional.empty();

        if (userId != null) {
            cart = cartRepository.findCartByUserIdAndStatus(userId, CartStatus.ACTIVE);
        } else if (guestId != null) {
            cart = cartRepository.findCartByGuestIdAndStatus(guestId, CartStatus.ACTIVE);
        }

        return cart.orElseThrow(() -> new NotFoundException("Cart not found"));
    }

    @Override
    @Transactional
    public void mergeCarts(Long userId, UUID guestId) {
        Cart guestCart = cartRepository.findCartWithProductsByGuestIdAndStatus(guestId, CartStatus.ACTIVE).orElse(null);

        if (guestCart == null) return;

        Cart userCart = cartRepository.findCartWithProductsByUserIdAndStatus(userId, CartStatus.ACTIVE).orElseGet(
                () -> this.createCart(userId)
        );

        Map<Long, CartItem> userItems = userCart.getCartItems()
                .stream()
                .collect(
                        Collectors.toMap(ci -> ci.getProduct().getId(), Function.identity())
                );

        for (CartItem guestItem : guestCart.getCartItems()) {
            Long productId = guestItem.getProduct().getId();
            CartItem existing = userItems.get(productId);
            if (existing != null) {
                existing.addToQuantity(guestItem.getQuantity());
            } else {
                CartItem newItem = new CartItem(userCart, guestItem.getProduct(), guestItem.getQuantity());
                userCart.getCartItems().add(newItem);
            }
        }

        guestCart.getCartItems().clear();
        guestCart.setStatus(CartStatus.MERGED);
        guestCart.setUser(userCart.getUser());
        guestCart.setGuestId(null);
        cartRepository.save(userCart);
    }

    @Override
    @Transactional
    public Cart createCart(Long userId) {
        cartRepository.findCartsByUserId(userId).forEach(cart -> {
            if (cart.getStatus() == CartStatus.ACTIVE) cart.setStatus(CartStatus.ABANDONED);
        });
        User user = userRepository.getReferenceById(userId);
        Cart cart = new Cart(user);
        return cartRepository.save(cart);
    }

    @Override
    public Cart createGuestCart() {
        return cartRepository.save(new Cart(UUID.randomUUID()));
    }

    @Override
    public Cart createGuestCart(UUID guestId) {
        if (guestId == null) {
            throw new InternalErrorException("Error on cart creation");
        }
        return cartRepository.save(new Cart(guestId));
    }

    @Override
    @Transactional
    public ShoppingCart addItemToCart(Long userId, UUID guestId, AddCartItemRequest item) {
        Cart cart = getOrCreateCart(userId, guestId);

        Product product = productService.getById(item.getProductId());

        if (product.getStock() < item.getQuantity()) {
            throw new OrderInsufficientStockException("Stock insuficiente: " + product.getName() + ".");
        }

        Optional<CartItem> repeated = this.findCartItemByProductId(cart, product.getId());

        if (repeated.isPresent()) {
            if (product.getStock() < repeated.get().getQuantity() + item.getQuantity()) {
                throw new OrderInsufficientStockException("Stock insuficiente" + product.getName() + ".");
            }
            repeated.get().addToQuantity(item.getQuantity());
        } else {
            CartItem newCartItem = new CartItem(cart, product, item.getQuantity());
            cart.getCartItems().add(newCartItem);
        }

        cartRepository.save(cart);
        return cartMapper.toDto(cart);
    }

    @Override
    @Transactional
    public ShoppingCart removeItemFromCart(Long userId, UUID guestId, Long productId) {
        Cart cart = getCart(userId, guestId);

        CartItem foundItem = findCartItemByProductId(cart, productId).orElseThrow(
                () -> new NotFoundException("Cart item not found")
        );

        cart.getCartItems().remove(foundItem);

        return cartMapper.toDto(cart);
    }

    @Override
    @Transactional
    public ShoppingCart updateItemQuantity(Long userId, UUID guestId, Long productId, Integer quantity) {
        Cart cart = getCart(userId, guestId);

        CartItem cartItem = findCartItemByProductId(cart, productId).orElseThrow(
                () -> new NotFoundException("Cart item not found"));

        if (quantity == 0) {
            cart.getCartItems().remove(cartItem);
        } else {
            if (cartItem.getProduct().getStock() < quantity) {
                throw new OrderInsufficientStockException(
                        "Stock insuficiente: " + cartItem.getProduct().getName() + "."
                );
            }
            cartItem.setQuantity(quantity);
        }
        return cartMapper.toDto(cart);
    }

    private Optional<CartItem> findCartItemByProductId(Cart cart, Long productId) {
        return cart.getCartItems()
                .stream()
                .filter(i -> i.getProduct().getId().equals(productId))
                .findFirst();
    }

    private Cart getOrCreateCart(Long userId, UUID guestId) {
        if (userId != null) {
            return cartRepository.findWithItemsByUserIdAndStatus(userId, CartStatus.ACTIVE)
                    .orElseGet(() -> createCart(userId));
        }

        if (guestId != null) {
            return cartRepository.findWithItemsByGuestIdAndStatus(guestId, CartStatus.ACTIVE)
                    .orElseGet(() -> createGuestCart(guestId));
        }

        return createGuestCart();
    }
}
