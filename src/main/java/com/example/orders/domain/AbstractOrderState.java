package com.example.orders.domain;

/** Shared behaviour for all states; concrete states override only what differs. */
abstract class AbstractOrderState implements OrderState {

    @Override
    public void moveTo(Order order, OrderStatus target) {
        if (target == OrderStatus.CANCELLED) {
            cancel(order);   // cancellation always goes through the state's cancel rule
            return;
        }
        if (!canTransitionTo(target)) {
            throw new InvalidOrderStateException(order.getId(), status(), target);
        }
        order.changeStatus(target);
    }

    @Override
    public void cancel(Order order) {
        throw new InvalidOrderStateException(
                "Order %s cannot be cancelled because it is %s; only PENDING orders can be cancelled"
                        .formatted(order.getId(), status()));
    }
}
