package org.adso.minimarket.service;

import org.adso.minimarket.dto.CheckoutRequest;
import org.adso.minimarket.dto.OrderDetails;
import org.adso.minimarket.dto.OrderSummary;
import org.adso.minimarket.exception.BadRequestException;
import org.adso.minimarket.exception.NotFoundException;
import org.adso.minimarket.exception.OrderInsufficientStockException;
import org.adso.minimarket.mappers.OrderMapper;
import org.adso.minimarket.models.cart.Cart;
import org.adso.minimarket.models.cart.CartItem;
import org.adso.minimarket.models.cart.CartStatus;
import org.adso.minimarket.models.inventory.TransactionType;
import org.adso.minimarket.models.order.Order;
import org.adso.minimarket.models.order.OrderItem;
import org.adso.minimarket.models.order.OrderStatus;
import org.adso.minimarket.models.product.Product;
import org.adso.minimarket.models.user.User;
import org.adso.minimarket.repository.jpa.OrderRepository;
import org.adso.minimarket.repository.jpa.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CartService cartService;
    private final InventoryService inventoryService;
    private final OrderMapper orderMapper;

    public OrderServiceImpl(OrderRepository orderRepository,
                            ProductRepository productRepository,
                            CartService cartService,
                            InventoryService inventoryService,
                            OrderMapper orderMapper) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.cartService = cartService;
        this.inventoryService = inventoryService;
        this.orderMapper = orderMapper;
    }

    @Override
    @Transactional
    public OrderDetails placeOrder(User user, CheckoutRequest checkoutRequest) {
        Order order = createOrder(user, checkoutRequest);
        return orderMapper.toOrderDetailsDto(order);
    }

    @Override
    public OrderSummary getOrderById(UUID orderId, Long userId) {
        Order order = orderRepository.findOrderByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new NotFoundException("Orden no encontrada"));
        return orderMapper.toOrderSummaryDto(order);
    }

    @Override
    public List<OrderSummary> getUserOrders(Long userId) {
        List<Order> orders = orderRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
        return orderMapper.toOrderSummaryDtoList(orders);
    }

    private Order createOrder(User user, CheckoutRequest shippingInfo) {
        Cart cart = cartService.getCart(user.getId(), null);
        if (cart.getCartItems().isEmpty()) {
            throw new BadRequestException("Orden invalida: El carrito esta vacio");
        }

        Order order = new Order();
        order.setUser(user);
        order.setEmail(user.getEmail());

        if (shippingInfo != null) {
            order.setShippingFullName(shippingInfo.getShippingFullName());
            order.setShippingAddressLine(shippingInfo.getShippingAddressLine());
            order.setShippingCity(shippingInfo.getShippingCity());
            order.setShippingZipCode(shippingInfo.getShippingZipCode());
            order.setShippingCountry(shippingInfo.getShippingCountry());
        }

        List<Long> productIds = cart.getCartItems().stream()
                .map(ci -> ci.getProduct().getId())
                .sorted()
                .collect(Collectors.toList());

        Map<Long, Product> productMap = productRepository.findForUpdateAllByIdIn(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        BigDecimal total = BigDecimal.ZERO;

        for (CartItem ci : cart.getCartItems()) {
            Product product = productMap.get(ci.getProduct().getId());

            if (product.getStock() < ci.getQuantity()) {
                throw new OrderInsufficientStockException("Stock insuficiente: " + product.getName() + ".");
            }

            inventoryService.adjustStock(product.getId(), -ci.getQuantity(),
                    TransactionType.SALE,
                    "Order placed: " + order.getId());

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(ci.getQuantity());
            orderItem.setPrice(product.getPrice());
            orderItem.setSubTotal(product.getPrice()
                    .multiply(BigDecimal.valueOf(ci.getQuantity()))
                    .setScale(2, RoundingMode.HALF_UP));
            order.getOrderItems().add(orderItem);
            total = total.add(orderItem.getSubTotal()).setScale(2, RoundingMode.HALF_UP);
        }

        cart.getCartItems().clear();
        cart.setStatus(CartStatus.COMPLETE);
        order.setTotalAmount(total);
        order.setStatus(OrderStatus.PENDING);
        return orderRepository.save(order);
    }
}
