package com.example.orders.api.dto;

import com.example.orders.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** <b>DTO</b>: the API contract, decoupled from the JPA entity. Built by {@code OrderMapper}. */
public record OrderResponse(
        Long id,
        String customerId,
        OrderStatus status,
        BigDecimal totalAmount,
        boolean cancellable,
        List<OrderStatus> allowedNextStatuses,
        Instant createdAt,
        Instant updatedAt,
        List<Item> items
) {
    public record Item(Long id, String productId, String productName,
                       int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {}
}
