package com.example.orders.domain;

import java.util.EnumMap;
import java.util.Map;

/** <b>Factory</b> (with a <b>Flyweight</b>-style cache): maps a persisted status to its shared state object. */
public final class OrderStateFactory {

    private static final Map<OrderStatus, OrderState> STATES = new EnumMap<>(OrderStatus.class);

    static {
        STATES.put(OrderStatus.PENDING, PendingState.INSTANCE);
        STATES.put(OrderStatus.PROCESSING, ProcessingState.INSTANCE);
        STATES.put(OrderStatus.SHIPPED, ShippedState.INSTANCE);
        STATES.put(OrderStatus.DELIVERED, DeliveredState.INSTANCE);
        STATES.put(OrderStatus.CANCELLED, CancelledState.INSTANCE);
    }

    private OrderStateFactory() {}

    public static OrderState forStatus(OrderStatus status) {
        OrderState state = STATES.get(status);
        if (state == null) {
            throw new IllegalArgumentException("No state registered for " + status);
        }
        return state;
    }
}
