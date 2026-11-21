package com.example.orders.domain.pricing;

import com.example.orders.domain.OrderItem;

import java.math.BigDecimal;
import java.util.List;

/** <b>Strategy pattern</b>: interchangeable algorithms for computing an order total. */
public interface PricingStrategy {

    /** Name used to select the strategy in configuration ({@code orders.pricing.strategy}). */
    String name();

    BigDecimal calculateTotal(List<OrderItem> items);
}
