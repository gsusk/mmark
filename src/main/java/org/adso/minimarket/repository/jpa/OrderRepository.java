package org.adso.minimarket.repository.jpa;

import org.adso.minimarket.models.order.Order;
import org.adso.minimarket.models.order.OrderStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    @EntityGraph(attributePaths = {"user", "orderItems", "orderItems.product"})
    Optional<Order> findOrderByIdAndUserId(UUID id, Long userId);

    @EntityGraph(attributePaths = {"user", "orderItems", "orderItems.product"})
    List<Order> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"user", "orderItems", "orderItems.product"})
    List<Order> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"orderItems", "orderItems.product"})
    List<Order> findAllByStatusAndCreatedAtBefore(OrderStatus status, LocalDateTime cutoff);
}
