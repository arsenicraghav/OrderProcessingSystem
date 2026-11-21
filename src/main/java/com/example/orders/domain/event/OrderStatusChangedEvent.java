package com.example.orders.domain.event;

import com.example.orders.domain.OrderStatus;

import java.time.Instant;

public record OrderStatusChangedEvent(Long orderId, String customerId,
                                      OrderStatus from, OrderStatus to, Instant occurredAt) {}
