package com.example.orders.domain;

import java.util.EnumSet;
import java.util.Set;

final class ShippedState extends AbstractOrderState {

    static final ShippedState INSTANCE = new ShippedState();

    private ShippedState() {}

    @Override public OrderStatus status() { return OrderStatus.SHIPPED; }

    @Override public Set<OrderStatus> allowedTransitions() { return EnumSet.of(OrderStatus.DELIVERED); }
}
