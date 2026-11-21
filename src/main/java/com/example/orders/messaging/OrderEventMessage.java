package com.example.orders.messaging;

import com.example.orders.domain.OrderStatus;
import com.example.orders.domain.event.OrderCreatedEvent;
import com.example.orders.domain.event.OrderStatusChangedEvent;
import com.example.orders.domain.event.PendingOrdersPromotedEvent;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Wire format for events sent to the {@code orders.events} queue. A stable, versioned contract
 * for other systems, decoupled from the internal domain event classes (<b>Adapter</b>/DTO).
 * <pre>
 * {"schemaVersion":1,"eventType":"ORDER_STATUS_CHANGED","orderId":7,"customerId":"c-1",
 *  "fromStatus":"PENDING","toStatus":"PROCESSING","occurredAt":"2026-11-21T03:30:00Z"}
 * </pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrderEventMessage(
        int schemaVersion,
        String eventType,
        Long orderId,
        String customerId,
        OrderStatus fromStatus,
        OrderStatus toStatus,
        BigDecimal totalAmount,
        Integer count,
        Instant occurredAt
) {
    public static final int SCHEMA_VERSION = 1;
    public static final String ORDER_CREATED = "ORDER_CREATED";
    public static final String ORDER_STATUS_CHANGED = "ORDER_STATUS_CHANGED";
    public static final String PENDING_ORDERS_PROMOTED = "PENDING_ORDERS_PROMOTED";

    static OrderEventMessage from(OrderCreatedEvent e) {
        return new OrderEventMessage(SCHEMA_VERSION, ORDER_CREATED, e.orderId(), e.customerId(),
                null, OrderStatus.PENDING, e.totalAmount(), null, e.occurredAt());
    }

    static OrderEventMessage from(OrderStatusChangedEvent e) {
        return new OrderEventMessage(SCHEMA_VERSION, ORDER_STATUS_CHANGED, e.orderId(), e.customerId(),
                e.from(), e.to(), null, null, e.occurredAt());
    }

    static OrderEventMessage from(PendingOrdersPromotedEvent e) {
        return new OrderEventMessage(SCHEMA_VERSION, PENDING_ORDERS_PROMOTED, null, null,
                OrderStatus.PENDING, OrderStatus.PROCESSING, null, e.count(), e.occurredAt());
    }
}
