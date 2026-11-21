package com.example.orders.service.handler;

import com.example.orders.domain.Order;
import com.example.orders.repository.OrderRepository;
import com.example.orders.service.command.CancelOrderCommand;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.Clock;

/**
 * Cancels an order. Only {@code PendingState} allows it. If the background job promotes the
 * order between load and flush, the version check fails and the caller gets a 409.
 */
@Component
public class CancelOrderHandler extends AbstractStatusChangeHandler<CancelOrderCommand> {

    public CancelOrderHandler(OrderRepository repository, ApplicationEventPublisher events, Clock clock) {
        super(repository, events, clock);
    }

    @Override
    public Class<CancelOrderCommand> commandType() {
        return CancelOrderCommand.class;
    }

    @Override
    protected Long orderId(CancelOrderCommand command) {
        return command.orderId();
    }

    @Override
    protected void applyChange(Order order, CancelOrderCommand command) {
        order.cancel();
    }
}
