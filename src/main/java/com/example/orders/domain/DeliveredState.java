package com.example.orders.domain;

import java.util.EnumSet;
import java.util.Set;

/** Terminal state. */
final class DeliveredState extends AbstractOrderState {

    static final DeliveredState INSTANCE = new DeliveredState();

    private DeliveredState() {}

    @Override public OrderStatus status() { return OrderStatus.DELIVERED; }

    @Override public Set<OrderStatus> allowedTransitions() { return EnumSet.noneOf(OrderStatus.class); }
}
