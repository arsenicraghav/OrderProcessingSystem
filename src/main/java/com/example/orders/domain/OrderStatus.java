package com.example.orders.domain;

import java.util.Set;

/** Persisted status value. Behaviour lives in the matching {@link OrderState}. */
public enum OrderStatus {
    PENDING,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED;

    public OrderState state() {
        return OrderStateFactory.forStatus(this);
    }

    public Set<OrderStatus> allowedNext() {
        return state().allowedTransitions();
    }

    public boolean canTransitionTo(OrderStatus target) {
        return state().canTransitionTo(target);
    }
}
