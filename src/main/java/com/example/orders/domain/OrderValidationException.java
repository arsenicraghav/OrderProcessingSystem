package com.example.orders.domain;

/** A business rule rejected the order (raised by the validation chain). */
public class OrderValidationException extends RuntimeException {
    public OrderValidationException(String message) {
        super(message);
    }
}
