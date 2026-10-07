package com.interviewprep.order.dto;

import com.interviewprep.order.Order;
import com.interviewprep.order.OrderItem;
import com.interviewprep.order.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        OrderStatus status,
        List<Item> items,
        BigDecimal total,
        Instant createdAt) {

    public record Item(Long productId, int quantity, BigDecimal unitPrice) {

        static Item from(OrderItem item) {
            return new Item(item.getProductId(), item.getQuantity(), item.getUnitPrice());
        }
    }

    public static OrderResponse from(Order order) {
        BigDecimal total = order.getItems().stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                order.getItems().stream().map(Item::from).toList(),
                total,
                order.getCreatedAt());
    }
}
