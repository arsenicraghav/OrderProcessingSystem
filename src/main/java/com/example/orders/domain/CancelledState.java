package com.example.orders.domain;

import java.util.EnumSet;
import java.util.Set;

/** Terminal state. */
final class CancelledState extends AbstractOrderState {

    static final CancelledState INSTANCE = new CancelledState();

    private CancelledState() {}

    @Override public OrderStatus status() { return OrderStatus.CANCELLED; }

    @Override public Set<OrderStatus> allowedTransitions() { return EnumSet.noneOf(OrderStatus.class); }
}
