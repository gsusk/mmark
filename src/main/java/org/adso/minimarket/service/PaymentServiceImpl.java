package org.adso.minimarket.service;

import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentConfirmParams;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.adso.minimarket.config.StripeConfig;
import org.adso.minimarket.dto.CreatePaymentRequest;
import org.adso.minimarket.dto.CreatePaymentResponse;
import org.adso.minimarket.exception.BadRequestException;
import org.adso.minimarket.exception.InternalErrorException;
import org.adso.minimarket.exception.NotFoundException;
import org.adso.minimarket.models.order.Order;
import org.adso.minimarket.models.order.OrderStatus;
import org.adso.minimarket.models.payment.Payment;
import org.adso.minimarket.models.payment.PaymentStatus;
import org.adso.minimarket.repository.jpa.OrderRepository;
import org.adso.minimarket.repository.jpa.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final StripeConfig stripeConfig;

    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              OrderRepository orderRepository,
                              StripeConfig stripeConfig) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.stripeConfig = stripeConfig;
    }

    @Override
    @Transactional
    public CreatePaymentResponse createPayment(CreatePaymentRequest request) {
        Order order = orderRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException("Order not found: " + request.getId()));

        String paymentReference;
        String clientSecret = null;
        PaymentStatus paymentStatus;

        if (stripeConfig != null && stripeConfig.isConfigured()) {
            try {
                long amountInCents = request.getTotal().multiply(BigDecimal.valueOf(100)).longValue();
                PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                        .setAmount(amountInCents)
                        .setCurrency("usd")
                        .setDescription("Minimarket Order: " + order.getId())
                        .putMetadata("order_id", order.getId().toString())
                        .putMetadata("user_id", String.valueOf(request.getUserId()))
                        .setAutomaticPaymentMethods(
                                PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                        .setEnabled(true)
                                        .build()
                        )
                        .build();

                PaymentIntent intent = PaymentIntent.create(params);
                paymentReference = intent.getId();
                clientSecret = intent.getClientSecret();
                paymentStatus = mapStripeStatus(intent.getStatus());
                log.info("[PaymentService] Stripe PaymentIntent creado exitosamente: {}", paymentReference);
            } catch (StripeException e) {
                log.error("[PaymentService] Error al crear PaymentIntent en Stripe: {}", e.getMessage(), e);
                throw new InternalErrorException("Error al procesar pago con Stripe: " + e.getUserMessage());
            }
        } else {
            paymentReference = "PAY-" + UUID.randomUUID().toString().toUpperCase();
            paymentStatus = PaymentStatus.COMPLETED;
            log.info("[PaymentService] Stripe no configurado; operando con referencia simulada: {}", paymentReference);
        }

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(request.getTotal());
        payment.setCurrency("usd");
        payment.setPaymentReference(paymentReference);
        payment.setStatus(paymentStatus);

        paymentRepository.save(payment);

        if (paymentStatus == PaymentStatus.COMPLETED) {
            order.setStatus(OrderStatus.COMPLETED);
        } else {
            order.setStatus(OrderStatus.PENDING);
        }
        orderRepository.save(order);

        String pubKey = stripeConfig != null ? stripeConfig.getPublishableKey() : null;

        return CreatePaymentResponse.builder()
                .paymentReference(paymentReference)
                .clientSecret(clientSecret)
                .publishableKey(pubKey)
                .currency(payment.getCurrency())
                .amount(payment.getAmount())
                .status(payment.getStatus().name().toLowerCase())
                .orderId(order.getId())
                .build();
    }

    @Override
    @Transactional
    public CreatePaymentResponse getPaymentByOrderId(UUID orderId, Long userId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NotFoundException("Payment not found for order: " + orderId));

        if (!payment.getOrder().getUser().getId().equals(userId)) {
            throw new NotFoundException("Payment not found for order: " + orderId);
        }

        String clientSecret = null;

        if (payment.getStatus() == PaymentStatus.PENDING
                && stripeConfig != null
                && stripeConfig.isConfigured()
                && payment.getPaymentReference() != null
                && payment.getPaymentReference().startsWith("pi_")) {
            try {
                PaymentIntent intent = PaymentIntent.retrieve(payment.getPaymentReference());
                clientSecret = intent.getClientSecret();
                PaymentStatus syncStatus = mapStripeStatus(intent.getStatus());

                if (syncStatus != payment.getStatus()) {
                    payment.setStatus(syncStatus);
                    paymentRepository.save(payment);

                    if (syncStatus == PaymentStatus.COMPLETED) {
                        payment.getOrder().setStatus(OrderStatus.COMPLETED);
                        orderRepository.save(payment.getOrder());
                    } else if (syncStatus == PaymentStatus.FAILED) {
                        payment.getOrder().setStatus(OrderStatus.FAILED);
                        orderRepository.save(payment.getOrder());
                    }
                }
            } catch (StripeException e) {
                log.warn("[PaymentService] No fue posible sincronizar estado de Stripe para {}: {}",
                        payment.getPaymentReference(), e.getMessage());
            }
        }

        String pubKey = stripeConfig != null ? stripeConfig.getPublishableKey() : null;

        return CreatePaymentResponse.builder()
                .paymentReference(payment.getPaymentReference())
                .clientSecret(clientSecret)
                .publishableKey(pubKey)
                .currency(payment.getCurrency())
                .amount(payment.getAmount())
                .status(payment.getStatus().name().toLowerCase())
                .orderId(payment.getOrder().getId())
                .build();
    }

    @Override
    @Transactional
    public CreatePaymentResponse confirmPayment(UUID orderId, Long userId, String paymentMethodId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NotFoundException("Payment not found for order: " + orderId));

        if (!payment.getOrder().getUser().getId().equals(userId)) {
            throw new NotFoundException("Payment not found for order: " + orderId);
        }

        String clientSecret = null;

        if (stripeConfig != null
                && stripeConfig.isConfigured()
                && payment.getPaymentReference() != null
                && payment.getPaymentReference().startsWith("pi_")) {
            try {
                PaymentIntent intent = PaymentIntent.retrieve(payment.getPaymentReference());
                clientSecret = intent.getClientSecret();

                if (!"succeeded".equalsIgnoreCase(intent.getStatus())) {
                    String pm = (paymentMethodId != null && !paymentMethodId.isBlank()) ? paymentMethodId : "pm_card_visa";
                    PaymentIntentConfirmParams params = PaymentIntentConfirmParams.builder()
                            .setPaymentMethod(pm)
                            .build();
                    intent = intent.confirm(params);
                }

                PaymentStatus newStatus = mapStripeStatus(intent.getStatus());
                payment.setStatus(newStatus);
                paymentRepository.save(payment);

                if (newStatus == PaymentStatus.COMPLETED) {
                    payment.getOrder().setStatus(OrderStatus.COMPLETED);
                    orderRepository.save(payment.getOrder());
                } else if (newStatus == PaymentStatus.FAILED) {
                    payment.getOrder().setStatus(OrderStatus.FAILED);
                    orderRepository.save(payment.getOrder());
                }
                log.info("[PaymentService] PaymentIntent {} confirmado. Estado resultante: {}",
                        intent.getId(), intent.getStatus());
            } catch (StripeException e) {
                log.error("[PaymentService] Error al confirmar PaymentIntent {}: {}", payment.getPaymentReference(), e.getMessage());
                throw new InternalErrorException("Error al confirmar pago en Stripe: " + e.getUserMessage());
            }
        } else {
            payment.setStatus(PaymentStatus.COMPLETED);
            paymentRepository.save(payment);
            payment.getOrder().setStatus(OrderStatus.COMPLETED);
            orderRepository.save(payment.getOrder());
        }

        String pubKey = stripeConfig != null ? stripeConfig.getPublishableKey() : null;

        return CreatePaymentResponse.builder()
                .paymentReference(payment.getPaymentReference())
                .clientSecret(clientSecret)
                .publishableKey(pubKey)
                .currency(payment.getCurrency())
                .amount(payment.getAmount())
                .status(payment.getStatus().name().toLowerCase())
                .orderId(payment.getOrder().getId())
                .build();
    }

    @Override
    @Transactional
    public void processWebhook(String payload, String sigHeader) {
        Event event;
        try {
            if (stripeConfig != null && stripeConfig.getWebhookSecret() != null && !stripeConfig.getWebhookSecret().isBlank()) {
                event = Webhook.constructEvent(payload, sigHeader, stripeConfig.getWebhookSecret());
            } else {
                event = Event.GSON.fromJson(payload, Event.class);
            }
        } catch (Exception e) {
            log.error("[PaymentService] Error al verificar webhook de Stripe: {}", e.getMessage());
            throw new BadRequestException("Payload o firma de webhook inválida: " + e.getMessage());
        }

        if (event == null) {
            throw new BadRequestException("Evento nulo recibido");
        }

        StripeObject stripeObject = null;
        try {
            EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
            if (dataObjectDeserializer != null) {
                stripeObject = dataObjectDeserializer.getObject().orElse(null);
            }
        } catch (Exception e) {
            log.warn("[PaymentService] Excepción al deserializar data object de Stripe: {}", e.getMessage());
        }

        PaymentIntent paymentIntent = null;
        if (stripeObject instanceof PaymentIntent pi) {
            paymentIntent = pi;
        } else {
            try {
                com.google.gson.JsonObject root = com.google.gson.JsonParser.parseString(payload).getAsJsonObject();
                if (root.has("data") && root.getAsJsonObject("data").has("object")) {
                    paymentIntent = com.stripe.net.ApiResource.GSON.fromJson(
                            root.getAsJsonObject("data").get("object"),
                            PaymentIntent.class
                    );
                }
            } catch (Exception ignored) {}
        }

        if (paymentIntent != null && "payment_intent.succeeded".equals(event.getType())) {
            final String piId = paymentIntent.getId();
            paymentRepository.findByPaymentReference(piId).ifPresent(payment -> {
                payment.setStatus(PaymentStatus.COMPLETED);
                paymentRepository.save(payment);
                Order order = payment.getOrder();
                if (order != null) {
                    order.setStatus(OrderStatus.COMPLETED);
                    orderRepository.save(order);
                }
                log.info("[PaymentService] Webhook: PaymentIntent {} completado exitosamente.", piId);
            });
        } else if (paymentIntent != null && "payment_intent.payment_failed".equals(event.getType())) {
            final String piId = paymentIntent.getId();
            paymentRepository.findByPaymentReference(piId).ifPresent(payment -> {
                payment.setStatus(PaymentStatus.FAILED);
                paymentRepository.save(payment);
                Order order = payment.getOrder();
                if (order != null) {
                    order.setStatus(OrderStatus.FAILED);
                    orderRepository.save(order);
                }
                log.warn("[PaymentService] Webhook: PaymentIntent {} marcado como fallido.", piId);
            });
        } else {
            log.info("[PaymentService] Webhook recibido sin acción de pago: {}", event.getType());
        }
    }

    private PaymentStatus mapStripeStatus(String stripeStatus) {
        if (stripeStatus == null) return PaymentStatus.PENDING;
        return switch (stripeStatus.toLowerCase()) {
            case "succeeded" -> PaymentStatus.COMPLETED;
            case "canceled", "failed", "requires_payment_method" -> PaymentStatus.PENDING;
            default -> PaymentStatus.PENDING;
        };
    }
}
