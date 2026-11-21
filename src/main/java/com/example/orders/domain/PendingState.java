package com.example.orders.domain;

import java.util.EnumSet;
import java.util.Set;

/** Stateless <b>Singleton</b>: one shared instance per state. */
final class PendingState extends AbstractOrderState {

    static final PendingState INSTANCE = new PendingState();

    private PendingState() {}

    @Override public OrderStatus status() { return OrderStatus.PENDING; }

    @Override public Set<OrderStatus> allowedTransitions() {
        return EnumSet.of(OrderStatus.PROCESSING, OrderStatus.CANCELLED);
    }

    /** The only state in which a customer may cancel. */
    @Override
    public void cancel(Order order) {
        order.changeStatus(OrderStatus.CANCELLED);
    }
}
