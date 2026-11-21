package com.example.orders.domain;

public class InvalidOrderStateException extends RuntimeException {

    public InvalidOrderStateException(Long orderId, OrderStatus current, OrderStatus target) {
        super("Order %s cannot move from %s to %s".formatted(orderId, current, target));
    }

    public InvalidOrderStateException(String message) {
        super(message);
    }
}
