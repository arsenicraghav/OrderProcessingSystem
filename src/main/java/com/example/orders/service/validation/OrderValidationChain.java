package com.example.orders.service.validation;

import com.example.orders.service.command.CreateOrderCommand;
import org.springframework.stereotype.Component;

import java.util.List;

/** Assembles all {@link OrderValidator} beans (in {@code @Order} order) into one chain. */
@Component
public class OrderValidationChain {

    private final OrderValidator head;

    public OrderValidationChain(List<OrderValidator> validators) {
        if (validators.isEmpty()) {
            throw new IllegalStateException("At least one OrderValidator is required");
        }
        this.head = validators.get(0);
        OrderValidator current = head;
        for (int i = 1; i < validators.size(); i++) {
            current = current.linkWith(validators.get(i));
        }
    }

    public void validate(CreateOrderCommand command) {
        head.validate(command);
    }
}
