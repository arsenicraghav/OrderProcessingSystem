package com.example.orders.domain.pricing;

import com.example.orders.domain.Order;
import com.example.orders.domain.OrderItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Strategy pattern: same items, different algorithms. */
class PricingStrategyTest {

    private final List<OrderItem> items = List.of(
            new OrderItem("p-1", "Bolt", 10, new BigDecimal("1.00")),   // bulk line: 10.00
            new OrderItem("p-2", "Nut", 2, new BigDecimal("0.50")));    // normal line: 1.00

    @Test
    void standardSumsLines() {
        assertThat(new StandardPricingStrategy().calculateTotal(items)).isEqualByComparingTo("11.00");
    }

    @Test
    void bulkDiscountTakesTenPercentOffLargeLines() {
        assertThat(new BulkDiscountPricingStrategy().calculateTotal(items)).isEqualByComparingTo("10.00");
    }

    @Test
    void builderUsesTheChosenStrategy() {
        Order order = Order.builder().customerId("c")
                .item("p-1", "Bolt", 10, new BigDecimal("1.00"))
                .pricedWith(new BulkDiscountPricingStrategy())
                .build();
        assertThat(order.getTotalAmount()).isEqualByComparingTo("9.00");
    }
}
