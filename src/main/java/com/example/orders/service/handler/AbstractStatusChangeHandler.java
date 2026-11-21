package com.example.orders.service.handler;

import com.example.orders.domain.Order;
import com.example.orders.domain.OrderNotFoundException;
import com.example.orders.domain.OrderStatus;
import com.example.orders.domain.event.OrderStatusChangedEvent;
import com.example.orders.repository.OrderRepository;
import com.example.orders.service.command.Command;
import com.example.orders.service.command.CommandHandler;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * <b>Template Method pattern</b>: the fixed algorithm (load → apply change → flush with
 * optimistic lock check → publish event) lives here; subclasses only supply the order id
 * and the change itself.
 */
public abstract class AbstractStatusChangeHandler<C extends Command<Order>> implements CommandHandler<C, Order> {

    private final OrderRepository repository;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    protected AbstractStatusChangeHandler(OrderRepository repository, ApplicationEventPublisher events, Clock clock) {
        this.repository = repository;
        this.events = events;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Order handle(C command) {
        Long id = orderId(command);
        Order order = repository.findWithItemsById(id).orElseThrow(() -> new OrderNotFoundException(id));
        OrderStatus from = order.getStatus();

        applyChange(order, command);   // the State object enforces the rules

        // Flushing here checks @Version: if the background job (or another request) changed
        // the order since we loaded it, this fails instead of silently overwriting.
        repository.flush();

        events.publishEvent(new OrderStatusChangedEvent(
                order.getId(), order.getCustomerId(), from, order.getStatus(), Instant.now(clock)));
        return order;
    }

    protected abstract Long orderId(C command);

    protected abstract void applyChange(Order order, C command);
}
