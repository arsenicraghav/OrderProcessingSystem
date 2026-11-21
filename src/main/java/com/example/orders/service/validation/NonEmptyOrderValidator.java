package com.example.orders.service.validation;

import com.example.orders.domain.OrderValidationException;
import com.example.orders.service.command.CreateOrderCommand;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Defensive check for callers that bypass the HTTP layer's Bean Validation. */
@Component
@Order(1)
public class NonEmptyOrderValidator extends OrderValidator {

    @Override
    protected void check(CreateOrderCommand command) {
        if (command.customerId() == null || command.customerId().isBlank()) {
            throw new OrderValidationException("customerId is required");
        }
        if (command.lines().isEmpty()) {
            throw new OrderValidationException("An order needs at least one item");
        }
        for (CreateOrderCommand.Line line : command.lines()) {
            if (line.quantity() <= 0) {
                throw new OrderValidationException("Quantity must be positive for product " + line.productId());
            }
            if (line.unitPrice() == null || line.unitPrice().signum() < 0) {
                throw new OrderValidationException("Unit price must be zero or more for product " + line.productId());
            }
        }
    }
}
