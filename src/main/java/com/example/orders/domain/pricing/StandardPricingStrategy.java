package com.example.orders.domain.pricing;

import com.example.orders.domain.OrderItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Sum of quantity × unit price. The default. */
@Component
public class StandardPricingStrategy implements PricingStrategy {

    public static final String NAME = "standard";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public BigDecimal calculateTotal(List<OrderItem> items) {
        return items.stream()
                .map(OrderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
