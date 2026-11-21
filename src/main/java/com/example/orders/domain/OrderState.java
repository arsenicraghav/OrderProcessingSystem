package com.example.orders.domain;

import java.util.Set;

/**
 * <b>State pattern.</b> Each order status has its own state object that decides which
 * operations are legal, so lifecycle rules live in one place instead of in if/else chains
 * spread across services.
 * <pre>
 *   PENDING ──► PROCESSING ──► SHIPPED ──► DELIVERED
 *      │
 *      └──► CANCELLED
 * </pre>
 */
public interface OrderState {

    OrderStatus status();

    Set<OrderStatus> allowedTransitions();

    /** Moves {@code order} to {@code target}, or throws if this state does not allow it. */
    void moveTo(Order order, OrderStatus target);

    /** Cancels {@code order}, or throws if this state does not allow it. */
    void cancel(Order order);

    default boolean canTransitionTo(OrderStatus target) {
        return allowedTransitions().contains(target);
    }

    default boolean isCancellable() {
        return canTransitionTo(OrderStatus.CANCELLED);
    }

    default boolean isTerminal() {
        return allowedTransitions().isEmpty();
    }
}
