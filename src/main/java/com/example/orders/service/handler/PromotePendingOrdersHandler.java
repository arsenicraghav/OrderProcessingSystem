package com.example.orders.service.handler;

import com.example.orders.domain.OrderStatus;
import com.example.orders.domain.event.PendingOrdersPromotedEvent;
import com.example.orders.repository.OrderRepository;
import com.example.orders.service.command.CommandHandler;
import com.example.orders.service.command.PromotePendingOrdersCommand;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/** Moves every PENDING order to PROCESSING with one SQL UPDATE. */
@Component
public class PromotePendingOrdersHandler implements CommandHandler<PromotePendingOrdersCommand, Integer> {

    private final OrderRepository repository;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public PromotePendingOrdersHandler(OrderRepository repository, ApplicationEventPublisher events, Clock clock) {
        this.repository = repository;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public Class<PromotePendingOrdersCommand> commandType() {
        return PromotePendingOrdersCommand.class;
    }

    @Override
    @Transactional
    public Integer handle(PromotePendingOrdersCommand command) {
        // Same rule the State pattern enforces, applied set-wise for efficiency.
        if (!OrderStatus.PENDING.canTransitionTo(OrderStatus.PROCESSING)) {
            throw new IllegalStateException("Lifecycle no longer allows PENDING -> PROCESSING");
        }
        Instant now = Instant.now(clock);
        int count = repository.bulkTransition(OrderStatus.PENDING, OrderStatus.PROCESSING, now);
        if (count > 0) {
            events.publishEvent(new PendingOrdersPromotedEvent(count, now));
        }
        return count;
    }
}
