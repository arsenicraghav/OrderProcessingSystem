package com.example.orders.service.command;

import com.example.orders.domain.Order;
import com.example.orders.domain.OrderStatus;

public record ChangeOrderStatusCommand(Long orderId, OrderStatus target) implements Command<Order> {}
