package org.adso.minimarket.mappers;

import org.adso.minimarket.dto.OrderDetails;
import org.adso.minimarket.dto.OrderItemSummary;
import org.adso.minimarket.dto.OrderSummary;
import org.adso.minimarket.models.order.Order;
import org.adso.minimarket.models.order.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "orderId", source = "id")
    @Mapping(target = "userId", expression = "java(order.getUser().getId())")
    @Mapping(target = "status", expression = "java(order.getStatus().name().toLowerCase())")
    @Mapping(target = "items", source = "orderItems")
    OrderSummary toOrderSummaryDto(Order order);

    @Mapping(target = "userId", expression = "java(order.getUser().getId())")
    @Mapping(target = "status", expression = "java(order.getStatus().name().toLowerCase())")
    @Mapping(target = "total", source = "totalAmount")
    @Mapping(target = "items", source = "orderItems")
    OrderDetails toOrderDetailsDto(Order order);

    List<OrderSummary> toOrderSummaryDtoList(List<Order> orders);

    @Mapping(target = "productName", source = "product.name")
    OrderItemSummary toOrderItemSummaryDto(OrderItem orderItem);
}
