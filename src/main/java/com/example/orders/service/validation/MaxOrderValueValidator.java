package com.example.orders.service.validation;

import com.example.orders.domain.OrderValidationException;
import com.example.orders.service.command.CreateOrderCommand;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Rejects orders above a configurable value (fraud / fat-finger guard). */
@Component
@Order(3)
public class MaxOrderValueValidator extends OrderValidator {

    private final BigDecimal maxOrderValue;

    public MaxOrderValueValidator(@Value("${orders.validation.max-order-value:1000000}") BigDecimal maxOrderValue) {
        this.maxOrderValue = maxOrderValue;
    }

    @Override
    protected void check(CreateOrderCommand command) {
        BigDecimal gross = command.lines().stream()
                .map(l -> l.unitPrice().multiply(BigDecimal.valueOf(l.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (gross.compareTo(maxOrderValue) > 0) {
            throw new OrderValidationException(
                    "Order value %s exceeds the maximum of %s".formatted(gross.toPlainString(), maxOrderValue.toPlainString()));
        }
    }
}
