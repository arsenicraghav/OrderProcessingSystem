package com.example.orders.domain.event;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderCreatedEvent(Long orderId, String customerId, BigDecimal totalAmount, Instant occurredAt) {}
