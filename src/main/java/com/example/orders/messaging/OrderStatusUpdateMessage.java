package com.example.orders.messaging;

import com.example.orders.domain.OrderStatus;

/** Inbound message on {@code orders.status-updates}, e.g. from a warehouse or shipping system. */
public record OrderStatusUpdateMessage(Long orderId, OrderStatus status) {}
