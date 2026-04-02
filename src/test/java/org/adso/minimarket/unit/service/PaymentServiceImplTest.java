package org.adso.minimarket.unit.service;

import org.adso.minimarket.dto.CreatePaymentRequest;
import org.adso.minimarket.dto.CreatePaymentResponse;
import org.adso.minimarket.exception.NotFoundException;
import org.adso.minimarket.models.order.Order;
import org.adso.minimarket.models.order.OrderStatus;
import org.adso.minimarket.models.payment.Payment;
import org.adso.minimarket.models.payment.PaymentStatus;
import org.adso.minimarket.models.user.User;
import org.adso.minimarket.repository.jpa.OrderRepository;
import org.adso.minimarket.repository.jpa.PaymentRepository;
import org.adso.minimarket.service.PaymentServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    private Order buildOrder(UUID id, Long userId) {
        User user = new User("Test", "User", "test@test.com", "pass");
        ReflectionTestUtils.setField(user, "id", userId);

        Order order = new Order();
        ReflectionTestUtils.setField(order, "id", id);
        order.setEmail("test@test.com");
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(new BigDecimal("150.00"));
        return order;
    }

    private CreatePaymentRequest buildPaymentRequest(UUID orderId, Long userId) {
        return CreatePaymentRequest.builder()
                .id(orderId)
                .email("test@test.com")
                .userId(userId)
                .total(new BigDecimal("150.00"))
                .status("PENDING")
                .shippingFullName("Test User")
                .shippingAddressLine("Calle 123")
                .shippingCity("Bogota")
                .shippingZipCode("110111")
                .shippingCountry("Colombia")
                .build();
    }

    @Test
    @DisplayName("Crear pago cuando el pedido se encuentra crea el pago y completa el pedido")
    void crearPago_cuandoPedidoSeEncuentra_creaPagoYCompletaPedido() {
        UUID orderId = UUID.randomUUID();
        Order order = buildOrder(orderId, 1L);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        CreatePaymentResponse result = paymentService.createPayment(buildPaymentRequest(orderId, 1L));

        assertNotNull(result);
        assertTrue(result.getPaymentReference().startsWith("PAY-"));
        assertEquals("usd", result.getCurrency());
        assertEquals(new BigDecimal("150.00"), result.getAmount());
        assertEquals("completed", result.getStatus());
        assertEquals(orderId, result.getOrderId());
        assertEquals(OrderStatus.COMPLETED, order.getStatus());

        verify(paymentRepository).save(any(Payment.class));
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("Crear pago cuando el pedido no se encuentra lanza NotFoundException")
    void crearPago_cuandoPedidoNoSeEncuentra_lanzaNotFoundException() {
        UUID randomId = UUID.randomUUID();
        when(orderRepository.findById(randomId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> paymentService.createPayment(buildPaymentRequest(randomId, 1L)));

        verifyNoInteractions(paymentRepository);
    }

    @Test
    @DisplayName("Crear pago genera referencias de pago unicas")
    void crearPago_generaReferenciasDePagoUnicas() {
        UUID orderId1 = UUID.randomUUID();
        UUID orderId2 = UUID.randomUUID();
        Order order1 = buildOrder(orderId1, 1L);
        Order order2 = buildOrder(orderId2, 2L);

        when(orderRepository.findById(orderId1)).thenReturn(Optional.of(order1));
        when(orderRepository.findById(orderId2)).thenReturn(Optional.of(order2));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String ref1 = paymentService.createPayment(buildPaymentRequest(orderId1, 1L)).getPaymentReference();
        String ref2 = paymentService.createPayment(buildPaymentRequest(orderId2, 2L)).getPaymentReference();

        assertNotEquals(ref1, ref2);
    }

    @Test
    @DisplayName("Obtener pago por OrderId cuando existe retorna la respuesta del pago")
    void obtenerPagoPorOrderId_cuandoExiste_retornaRespuestaPago() {
        UUID orderId = UUID.randomUUID();
        Order order = buildOrder(orderId, 1L);

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(new BigDecimal("150.00"));
        payment.setCurrency("usd");
        payment.setPaymentReference("PAY-ABCDEFGH");
        payment.setStatus(PaymentStatus.COMPLETED);

        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));

        CreatePaymentResponse result = paymentService.getPaymentByOrderId(orderId, 1L);

        assertEquals("PAY-ABCDEFGH", result.getPaymentReference());
        assertEquals("completed", result.getStatus());
        assertEquals(orderId, result.getOrderId());
    }

    @Test
    @DisplayName("Obtener pago por OrderId cuando el pago no se encuentra lanza NotFoundException")
    void obtenerPagoPorOrderId_cuandoPagoNoSeEncuentra_lanzaNotFoundException() {
        UUID orderId = UUID.randomUUID();
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> paymentService.getPaymentByOrderId(orderId, 1L));
    }

    @Test
    @DisplayName("Obtener pago por OrderId cuando el ID de usuario no coincide lanza NotFoundException")
    void obtenerPagoPorOrderId_cuandoIdUsuarioNoCoincide_lanzaNotFoundException() {
        UUID orderId = UUID.randomUUID();
        Order order = buildOrder(orderId, 1L);

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(new BigDecimal("150.00"));
        payment.setCurrency("usd");
        payment.setPaymentReference("PAY-XYZ");
        payment.setStatus(PaymentStatus.COMPLETED);

        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));

        assertThrows(NotFoundException.class,
                () -> paymentService.getPaymentByOrderId(orderId, 999L));
    }
}
