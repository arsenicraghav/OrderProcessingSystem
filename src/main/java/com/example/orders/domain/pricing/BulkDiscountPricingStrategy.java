package com.example.orders.domain.pricing;

import com.example.orders.domain.OrderItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** 10% off any line with 10 or more units. Enable with {@code orders.pricing.strategy=bulk-discount}. */
@Component
public class BulkDiscountPricingStrategy implements PricingStrategy {

    public static final String NAME = "bulk-discount";
    static final int BULK_THRESHOLD = 10;
    static final BigDecimal DISCOUNT_FACTOR = new BigDecimal("0.90");

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public BigDecimal calculateTotal(List<OrderItem> items) {
        return items.stream()
                .map(i -> i.getQuantity() >= BULK_THRESHOLD
                        ? i.getLineTotal().multiply(DISCOUNT_FACTOR)
                        : i.getLineTotal())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
