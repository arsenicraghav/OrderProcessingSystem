package com.example.orders.service.handler;

import com.example.orders.domain.Order;
import com.example.orders.domain.event.OrderCreatedEvent;
import com.example.orders.repository.OrderRepository;
import com.example.orders.service.command.CommandHandler;
import com.example.orders.service.command.CreateOrderCommand;
import com.example.orders.service.factory.OrderFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Component
public class CreateOrderHandler implements CommandHandler<CreateOrderCommand, Order> {

    private final OrderFactory orderFactory;
    private final OrderRepository repository;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public CreateOrderHandler(OrderFactory orderFactory, OrderRepository repository,
                              ApplicationEventPublisher events, Clock clock) {
        this.orderFactory = orderFactory;
        this.repository = repository;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public Class<CreateOrderCommand> commandType() {
        return CreateOrderCommand.class;
    }

    @Override
    @Transactional
    public Order handle(CreateOrderCommand command) {
        Order saved = repository.save(orderFactory.create(command));
        events.publishEvent(new OrderCreatedEvent(
                saved.getId(), saved.getCustomerId(), saved.getTotalAmount(), Instant.now(clock)));
        return saved;
    }
}
