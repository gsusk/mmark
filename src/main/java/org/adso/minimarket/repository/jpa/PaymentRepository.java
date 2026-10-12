package org.adso.minimarket.repository.jpa;

import org.adso.minimarket.models.payment.Payment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    @EntityGraph(attributePaths = {"order", "order.user"})
    Optional<Payment> findByOrderId(UUID orderId);

    @EntityGraph(attributePaths = {"order", "order.user"})
    Optional<Payment> findByPaymentReference(String paymentReference);
}
