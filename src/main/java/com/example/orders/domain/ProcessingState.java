package com.example.orders.domain;

import java.util.EnumSet;
import java.util.Set;

final class ProcessingState extends AbstractOrderState {

    static final ProcessingState INSTANCE = new ProcessingState();

    private ProcessingState() {}

    @Override public OrderStatus status() { return OrderStatus.PROCESSING; }

    @Override public Set<OrderStatus> allowedTransitions() { return EnumSet.of(OrderStatus.SHIPPED); }
}
