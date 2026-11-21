package com.example.orders.service.command;

import com.example.orders.domain.Order;

public record CancelOrderCommand(Long orderId) implements Command<Order> {}
