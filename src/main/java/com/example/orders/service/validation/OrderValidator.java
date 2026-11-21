package com.example.orders.service.validation;

import com.example.orders.service.command.CreateOrderCommand;

/**
 * <b>Chain of Responsibility</b>: each validator checks one business rule and passes the
 * command to the next link. New rules are added as new beans, without touching existing ones.
 */
public abstract class OrderValidator {

    private OrderValidator next;

    /** Links {@code next} after this validator and returns it, so links can be chained fluently. */
    public OrderValidator linkWith(OrderValidator next) {
        this.next = next;
        return next;
    }

    public final void validate(CreateOrderCommand command) {
        check(command);
        if (next != null) {
            next.validate(command);
        }
    }

    /** Throws {@link com.example.orders.domain.OrderValidationException} if the rule is broken. */
    protected abstract void check(CreateOrderCommand command);
}
