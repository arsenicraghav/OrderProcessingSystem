package com.example.orders.service.command;

import com.example.orders.domain.Order;

import java.math.BigDecimal;
import java.util.List;

public record CreateOrderCommand(String customerId, List<Line> lines) implements Command<Order> {

    public CreateOrderCommand {
        lines = lines == null ? List.of() : List.copyOf(lines);
    }

    public record Line(String productId, String productName, int quantity, BigDecimal unitPrice) {}
}
