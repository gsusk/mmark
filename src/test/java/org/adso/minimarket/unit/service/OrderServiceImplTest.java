package org.adso.minimarket.unit.service;

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
import org.adso.minimarket.models.order.OrderStatus;
import org.adso.minimarket.models.product.Product;
import org.adso.minimarket.models.user.User;
import org.adso.minimarket.repository.jpa.OrderRepository;
import org.adso.minimarket.repository.jpa.ProductRepository;
import org.adso.minimarket.service.CartService;
import org.adso.minimarket.service.InventoryService;
import org.adso.minimarket.service.OrderServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @InjectMocks
    private OrderServiceImpl orderService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CartService cartService;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private OrderMapper orderMapper;

    private User buildUser(Long id) {
        User u = new User("Test", "User", "test@test.com", "pass");
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }

    private Product buildProduct(Long id, String name, int stock, BigDecimal price) {
        Product p = new Product(name, "desc", price, stock, null, null, new HashMap<>());
        ReflectionTestUtils.setField(p, "id", id);
        return p;
    }

    private CheckoutRequest buildShippingInfo() {
        return CheckoutRequest.builder()
                .shippingFullName("Test User")
                .shippingAddressLine("Calle 123")
                .shippingCity("Bogota")
                .shippingZipCode("110111")
                .shippingCountry("Colombia")
                .build();
    }

    @Test
    @DisplayName("Realizar pedido con carrito válido crea el pedido exitosamente")
    void realizarPedido_conCarritoValido_creaPedidoExitosamente() {
        User user = buildUser(1L);
        Product product = buildProduct(10L, "Camisa", 20, new BigDecimal("50.00"));

        Cart cart = new Cart(user);
        ReflectionTestUtils.setField(cart, "id", 1L);
        CartItem item = new CartItem(cart, product, 2);
        cart.getCartItems().add(item);

        Order savedOrder = new Order();
        ReflectionTestUtils.setField(savedOrder, "id", UUID.randomUUID());
        savedOrder.setStatus(OrderStatus.PENDING);
        savedOrder.setEmail("test@test.com");
        savedOrder.setUser(user);

        OrderDetails expected = new OrderDetails(savedOrder.getId(), "test@test.com", 1L, "PENDING",
                new BigDecimal("100.00"), List.of(), null);

        when(cartService.getCart(1L, null)).thenReturn(cart);
        when(productRepository.findForUpdateAllByIdIn(any())).thenReturn(List.of(product));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(orderMapper.toOrderDetailsDto(any(Order.class))).thenReturn(expected);

        OrderDetails result = orderService.placeOrder(user, buildShippingInfo());

        assertNotNull(result);
        assertEquals("PENDING", result.getStatus());
        verify(inventoryService).adjustStock(eq(10L), eq(-2), eq(TransactionType.SALE), anyString());
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("Realizar pedido con carrito vacío lanza BadRequestException")
    void realizarPedido_conCarritoVacio_lanzaBadRequestException() {
        User user = buildUser(1L);
        Cart cart = new Cart(user);
        ReflectionTestUtils.setField(cart, "id", 1L);

        when(cartService.getCart(1L, null)).thenReturn(cart);

        assertThrows(BadRequestException.class,
                () -> orderService.placeOrder(user, buildShippingInfo()));

        verifyNoInteractions(productRepository, inventoryService, orderRepository);
    }

    @Test
    @DisplayName("Realizar pedido cuando el stock es insuficiente lanza OrderInsufficientStockException")
    void realizarPedido_cuandoStockInsuficiente_lanzaOrderInsufficientStockException() {
        User user = buildUser(1L);
        Product product = buildProduct(10L, "Camisa", 1, new BigDecimal("50.00"));

        Cart cart = new Cart(user);
        ReflectionTestUtils.setField(cart, "id", 1L);
        CartItem item = new CartItem(cart, product, 5);
        cart.getCartItems().add(item);

        when(cartService.getCart(1L, null)).thenReturn(cart);
        when(productRepository.findForUpdateAllByIdIn(any())).thenReturn(List.of(product));

        assertThrows(OrderInsufficientStockException.class,
                () -> orderService.placeOrder(user, buildShippingInfo()));

        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Obtener pedido por id cuando el pedido existe retorna el resumen del pedido")
    void obtenerPedidoPorId_cuandoPedidoExiste_retornaResumenPedido() {
        UUID orderId = UUID.randomUUID();
        Order order = new Order();
        ReflectionTestUtils.setField(order, "id", orderId);

        OrderSummary summary = new OrderSummary();
        summary.setOrderId(orderId);

        when(orderRepository.findOrderByIdAndUserId(orderId, 1L)).thenReturn(Optional.of(order));
        when(orderMapper.toOrderSummaryDto(order)).thenReturn(summary);

        OrderSummary result = orderService.getOrderById(orderId, 1L);

        assertEquals(orderId, result.getOrderId());
        verify(orderRepository).findOrderByIdAndUserId(orderId, 1L);
    }

    @Test
    @DisplayName("Obtener pedido por id cuando no se encuentra lanza NotFoundException")
    void obtenerPedidoPorId_cuandoNoSeEncuentra_lanzaNotFoundException() {
        UUID orderId = UUID.randomUUID();

        when(orderRepository.findOrderByIdAndUserId(orderId, 1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> orderService.getOrderById(orderId, 1L));
        verifyNoInteractions(orderMapper);
    }

    @Test
    @DisplayName("Obtener pedidos de usuario retorna lista mapeada")
    void obtenerPedidosDeUsuario_retornaListaMapeada() {
        Order o1 = new Order();
        Order o2 = new Order();
        OrderSummary s1 = new OrderSummary();
        OrderSummary s2 = new OrderSummary();

        when(orderRepository.findAllByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(o1, o2));
        when(orderMapper.toOrderSummaryDtoList(List.of(o1, o2))).thenReturn(List.of(s1, s2));

        List<OrderSummary> result = orderService.getUserOrders(1L);

        assertEquals(2, result.size());
        verify(orderRepository).findAllByUserIdOrderByCreatedAtDesc(1L);
    }

    @Test
    @DisplayName("Obtener pedidos de usuario cuando no hay pedidos retorna lista vacía")
    void obtenerPedidosDeUsuario_cuandoNoHayPedidos_retornaListaVacia() {
        when(orderRepository.findAllByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        when(orderMapper.toOrderSummaryDtoList(List.of())).thenReturn(List.of());

        List<OrderSummary> result = orderService.getUserOrders(1L);

        assertTrue(result.isEmpty());
    }
}
