package com.example.orders.domain.event;

import java.time.Instant;

/** Published by the background job; one event per run, not per order, to keep the bulk update cheap. */
public record PendingOrdersPromotedEvent(int count, Instant occurredAt) {}
