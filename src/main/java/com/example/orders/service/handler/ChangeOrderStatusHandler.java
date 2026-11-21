package com.example.orders.service.handler;

import com.example.orders.domain.Order;
import com.example.orders.repository.OrderRepository;
import com.example.orders.service.command.ChangeOrderStatusCommand;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.Clock;

@Component
public class ChangeOrderStatusHandler extends AbstractStatusChangeHandler<ChangeOrderStatusCommand> {

    public ChangeOrderStatusHandler(OrderRepository repository, ApplicationEventPublisher events, Clock clock) {
        super(repository, events, clock);
    }

    @Override
    public Class<ChangeOrderStatusCommand> commandType() {
        return ChangeOrderStatusCommand.class;
    }

    @Override
    protected Long orderId(ChangeOrderStatusCommand command) {
        return command.orderId();
    }

    @Override
    protected void applyChange(Order order, ChangeOrderStatusCommand command) {
        order.transitionTo(command.target());
    }
}
