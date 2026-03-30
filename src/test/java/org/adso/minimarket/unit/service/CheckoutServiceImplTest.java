package org.adso.minimarket.unit.service;

import org.adso.minimarket.dto.CheckoutRequest;
import org.adso.minimarket.dto.CreatePaymentRequest;
import org.adso.minimarket.dto.CreatePaymentResponse;
import org.adso.minimarket.dto.OrderDetails;
import org.adso.minimarket.models.order.OrderStatus;
import org.adso.minimarket.models.user.User;
import org.adso.minimarket.service.CheckoutServiceImpl;
import org.adso.minimarket.service.OrderService;
import org.adso.minimarket.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheckoutServiceImplTest {

    @InjectMocks
    private CheckoutServiceImpl checkoutService;

    @Mock
    private OrderService orderService;

    @Mock
    private PaymentService paymentService;

    private User buildUser(Long id) {
        User u = new User("Test", "User", "test@test.com", "pass");
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }

    private CheckoutRequest buildRequest() {
        return CheckoutRequest.builder()
                .shippingFullName("Test User")
                .shippingAddressLine("Calle 123")
                .shippingCity("Bogota")
                .shippingZipCode("110111")
                .shippingCountry("Colombia")
                .build();
    }

    @Test
    void processCheckout_withValidData_placesOrderAndCreatesPayment() {
        User user = buildUser(1L);
        CheckoutRequest request = buildRequest();
        UUID orderId = UUID.randomUUID();

        OrderDetails orderDetails = new OrderDetails(orderId, "test@test.com", 1L,
                OrderStatus.PENDING.name(), new BigDecimal("200.00"), List.of(), null);

        CreatePaymentResponse paymentResponse = CreatePaymentResponse.builder()
                .paymentReference("PAY-TESTXYZ")
                .currency("usd")
                .amount(new BigDecimal("200.00"))
                .status("completed")
                .orderId(orderId)
                .build();

        when(orderService.placeOrder(user, request)).thenReturn(orderDetails);
        when(paymentService.createPayment(any(CreatePaymentRequest.class))).thenReturn(paymentResponse);

        CreatePaymentResponse result = checkoutService.processCheckout(user, request);

        assertNotNull(result);
        assertEquals("PAY-TESTXYZ", result.getPaymentReference());
        assertEquals(orderId, result.getOrderId());
        assertEquals("completed", result.getStatus());

        verify(orderService).placeOrder(user, request);
        verify(paymentService).createPayment(any(CreatePaymentRequest.class));
    }

    @Test
    void processCheckout_propagatesCorrectShippingDataToPayment() {
        User user = buildUser(1L);
        CheckoutRequest request = buildRequest();
        UUID orderId = UUID.randomUUID();

        OrderDetails orderDetails = new OrderDetails(orderId, "test@test.com", 1L,
                OrderStatus.PENDING.name(), new BigDecimal("100.00"), List.of(), null);

        CreatePaymentResponse paymentResponse = CreatePaymentResponse.builder()
                .paymentReference("PAY-ABC")
                .currency("usd")
                .amount(new BigDecimal("100.00"))
                .status("completed")
                .orderId(orderId)
                .build();

        when(orderService.placeOrder(user, request)).thenReturn(orderDetails);
        when(paymentService.createPayment(any(CreatePaymentRequest.class))).thenReturn(paymentResponse);

        checkoutService.processCheckout(user, request);

        verify(paymentService).createPayment(argThat(payReq ->
                "Test User".equals(payReq.getShippingFullName()) &&
                "Calle 123".equals(payReq.getShippingAddressLine()) &&
                "Bogota".equals(payReq.getShippingCity()) &&
                "110111".equals(payReq.getShippingZipCode()) &&
                "Colombia".equals(payReq.getShippingCountry())
        ));
    }

    @Test
    void processCheckout_whenOrderServiceFails_doesNotCallPaymentService() {
        User user = buildUser(1L);
        CheckoutRequest request = buildRequest();

        when(orderService.placeOrder(any(), any())).thenThrow(new RuntimeException("Order failed"));

        assertThrows(RuntimeException.class, () -> checkoutService.processCheckout(user, request));

        verifyNoInteractions(paymentService);
    }
}
