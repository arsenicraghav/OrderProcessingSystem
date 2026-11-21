package com.example.orders.service.factory;

import com.example.orders.domain.Order;
import com.example.orders.service.command.CreateOrderCommand;
import com.example.orders.service.validation.OrderValidationChain;
import org.springframework.stereotype.Component;

/**
 * <b>Factory pattern</b>: the single place that turns a request into a new, validated,
 * priced {@link Order}. Combines the validation chain, the pricing strategy and the builder.
 */
@Component
public class OrderFactory {

    private final OrderValidationChain validationChain;
    private final PricingStrategyFactory pricingStrategies;

    public OrderFactory(OrderValidationChain validationChain, PricingStrategyFactory pricingStrategies) {
        this.validationChain = validationChain;
        this.pricingStrategies = pricingStrategies;
    }

    public Order create(CreateOrderCommand command) {
        validationChain.validate(command);
        Order.Builder builder = Order.builder()
                .customerId(command.customerId())
                .pricedWith(pricingStrategies.active());
        command.lines().forEach(l -> builder.item(l.productId(), l.productName(), l.quantity(), l.unitPrice()));
        return builder.build();
    }
}
