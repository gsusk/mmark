package org.adso.minimarket.unit.service;

import org.adso.minimarket.dto.AddCartItemRequest;
import org.adso.minimarket.dto.ShoppingCart;
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
import org.adso.minimarket.service.CartServiceImpl;
import org.adso.minimarket.service.ProductService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @InjectMocks
    private CartServiceImpl cartService;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CartMapper cartMapper;

    @Mock
    private ProductService productService;

    private Product buildProduct(Long id, String name, int stock, BigDecimal price) {
        Product p = new Product(name, "desc", price, stock, null, null, new HashMap<>());
        ReflectionTestUtils.setField(p, "id", id);
        return p;
    }

    private Cart buildCart(Long cartId) {
        User user = new User("Test", "User", "test@test.com", "pass");
        ReflectionTestUtils.setField(user, "id", 1L);
        Cart cart = new Cart(user);
        ReflectionTestUtils.setField(cart, "id", cartId);
        return cart;
    }

    @Test
    @DisplayName("Obtener carrito por ID de usuario retorna carrito activo")
    void obtenerCarrito_porIdUsuario_retornaCarritoActivo() {
        Cart cart = buildCart(1L);
        when(cartRepository.findCartByUserIdAndStatus(1L, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));

        Cart result = cartService.getCart(1L, null);

        assertNotNull(result);
        verify(cartRepository).findCartByUserIdAndStatus(1L, CartStatus.ACTIVE);
    }

    @Test
    @DisplayName("Obtener carrito porr ID de invitado retorna carrito activo")
    void obtenerCarrito_porIdInvitado_retornaCarritoActivo() {
        UUID guestId = UUID.randomUUID();
        Cart cart = new Cart(guestId);
        ReflectionTestUtils.setField(cart, "id", 2L);

        when(cartRepository.findCartByGuestIdAndStatus(guestId, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));

        Cart result = cartService.getCart(null, guestId);

        assertNotNull(result);
        verify(cartRepository).findCartByGuestIdAndStatus(guestId, CartStatus.ACTIVE);
    }

    @Test
    @DisplayName("Obtener carrito cuando no se encuentra lanza NotFoundException")
    void obtenerCarrito_cuandoNoSeEncuentra_lanzaNotFoundException() {
        when(cartRepository.findCartByUserIdAndStatus(any(), any())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> cartService.getCart(1L, null));
    }

    @Test
    @DisplayName("Obtener carrito de compras retorna DTO mapeado")
    void obtenerCarritoDeCompras_retornaDtoMapeado() {
        Cart cart = buildCart(1L);
        ShoppingCart dto = new ShoppingCart(Set.of(), "0.00", 0);

        when(cartRepository.findCartByUserIdAndStatus(1L, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(cartMapper.toDto(cart)).thenReturn(dto);

        ShoppingCart result = cartService.getShoppingCart(1L, null);

        assertNotNull(result);
        verify(cartMapper).toDto(cart);
    }

    @Test
    @DisplayName("Crear carrito abandona carritos activos existentes y crea uno nuevo")
    void crearCarrito_abandonaCarritosActivosExistentesYCreaUnoNuevo() {
        User user = new User("Test", "User", "test@test.com", "pass");
        ReflectionTestUtils.setField(user, "id", 1L);

        Cart existingActive = new Cart(user);
        existingActive.setStatus(CartStatus.ACTIVE);
        Cart newCart = new Cart(user);
        ReflectionTestUtils.setField(newCart, "id", 10L);

        when(cartRepository.findCartsByUserId(1L)).thenReturn(List.of(existingActive));
        when(userRepository.getReferenceById(1L)).thenReturn(user);
        when(cartRepository.save(any(Cart.class))).thenReturn(newCart);

        Cart result = cartService.createCart(1L);

        assertEquals(CartStatus.ABANDONED, existingActive.getStatus());
        assertNotNull(result);
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    @DisplayName("Agregar item al carrito producto nuevo agrega item exitosamente")
    void agregarItemAlCarrito_productoNuevo_agregaItemExitosamente() {
        Product product = buildProduct(5L, "Camisa", 10, new BigDecimal("25.00"));
        Cart cart = buildCart(1L);
        ShoppingCart dto = new ShoppingCart(Set.of(), "25.00", 1);

        when(cartRepository.findWithItemsByUserIdAndStatus(1L, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(productService.getById(5L)).thenReturn(product);
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        when(cartMapper.toDto(any(Cart.class))).thenReturn(dto);

        ShoppingCart result = cartService.addItemToCart(1L, null, new AddCartItemRequest(5L, 2));

        assertNotNull(result);
        verify(productService).getById(5L);
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    @DisplayName("Agregar item al carrito cuando hay stock insuficiente lanza excepción")
    void agregarItemAlCarrito_cuandoStockInsuficiente_lanzaExcepcion() {
        Product product = buildProduct(5L, "Camisa", 1, new BigDecimal("25.00"));
        Cart cart = buildCart(1L);

        when(cartRepository.findWithItemsByUserIdAndStatus(1L, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(productService.getById(5L)).thenReturn(product);

        assertThrows(OrderInsufficientStockException.class,
                () -> cartService.addItemToCart(1L, null, new AddCartItemRequest(5L, 5)));

        verify(cartRepository, never()).save(any());
    }

    @Test
    @DisplayName("Remover item del carrito cuando el item existe lo remueve y retorna el carrito")
    void removerItemDelCarrito_cuandoItemExiste_remueveYRetornaCarrito() {
        Product product = buildProduct(5L, "Camisa", 10, new BigDecimal("25.00"));
        Cart cart = buildCart(1L);
        CartItem cartItem = new CartItem(cart, product, 2);
        cart.getCartItems().add(cartItem);

        ShoppingCart dto = new ShoppingCart(Set.of(), "0.00", 0);

        when(cartRepository.findCartByUserIdAndStatus(1L, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(cartMapper.toDto(cart)).thenReturn(dto);

        ShoppingCart result = cartService.removeItemFromCart(1L, null, 5L);

        assertTrue(cart.getCartItems().isEmpty());
        assertNotNull(result);
    }

    @Test
    @DisplayName("Remover item del carrito cuando el item no se encuentra lanza NotFoundException")
    void removerItemDelCarrito_cuandoItemNoSeEncuentra_lanzaNotFoundException() {
        Cart cart = buildCart(1L);

        when(cartRepository.findCartByUserIdAndStatus(1L, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));

        assertThrows(NotFoundException.class,
                () -> cartService.removeItemFromCart(1L, null, 99L));
    }

    @Test
    @DisplayName("Actualizar cantidad item con cantidad válida actualiza el item")
    void actualizarCantidadItem_conCantidadValida_actualizaItem() {
        Product product = buildProduct(5L, "Camisa", 10, new BigDecimal("25.00"));
        Cart cart = buildCart(1L);
        CartItem cartItem = new CartItem(cart, product, 2);
        cart.getCartItems().add(cartItem);

        ShoppingCart dto = new ShoppingCart(Set.of(), "75.00", 1);

        when(cartRepository.findCartByUserIdAndStatus(1L, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(cartMapper.toDto(any(Cart.class))).thenReturn(dto);

        ShoppingCart result = cartService.updateItemQuantity(1L, null, 5L, 3);

        assertEquals(3, cartItem.getQuantity());
        assertNotNull(result);
    }

    @Test
    @DisplayName("Actualizar cantidad item con cantidad cero remueve el item")
    void actualizarCantidadItem_conCantidadCero_remueveItem() {
        Product product = buildProduct(5L, "Camisa", 10, new BigDecimal("25.00"));
        Cart cart = buildCart(1L);
        CartItem cartItem = new CartItem(cart, product, 2);
        cart.getCartItems().add(cartItem);

        ShoppingCart dto = new ShoppingCart(Set.of(), "0.00", 0);

        when(cartRepository.findCartByUserIdAndStatus(1L, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(cartMapper.toDto(any(Cart.class))).thenReturn(dto);

        cartService.updateItemQuantity(1L, null, 5L, 0);

        assertTrue(cart.getCartItems().isEmpty());
    }

    @Test
    @DisplayName("Actualizar cantidad item cuando excede el stock lanza laa excepción")
    void actualizarCantidadItem_cuandoExcedeStock_lanzaExcepcion() {
        Product product = buildProduct(5L, "Camisa", 3, new BigDecimal("25.00"));
        Cart cart = buildCart(1L);
        CartItem cartItem = new CartItem(cart, product, 1);
        cart.getCartItems().add(cartItem);

        when(cartRepository.findCartByUserIdAndStatus(1L, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));

        assertThrows(OrderInsufficientStockException.class,
                () -> cartService.updateItemQuantity(1L, null, 5L, 10));
    }
}
