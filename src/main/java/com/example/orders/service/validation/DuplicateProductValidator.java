package com.example.orders.service.validation;

import com.example.orders.domain.OrderValidationException;
import com.example.orders.service.command.CreateOrderCommand;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/** The same product must appear on one line only (send a higher quantity instead). */
@Component
@Order(2)
public class DuplicateProductValidator extends OrderValidator {

    @Override
    protected void check(CreateOrderCommand command) {
        Set<String> seen = new HashSet<>();
        for (CreateOrderCommand.Line line : command.lines()) {
            if (!seen.add(line.productId())) {
                throw new OrderValidationException(
                        "Product " + line.productId() + " appears more than once; combine it into one line");
            }
        }
    }
}
