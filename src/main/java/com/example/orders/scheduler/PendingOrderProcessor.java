package com.example.orders.scheduler;

import com.example.orders.service.command.CommandBus;
import com.example.orders.service.command.PromotePendingOrdersCommand;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Background job: every 5 minutes (configurable) move all PENDING orders to PROCESSING. */
@Component
@ConditionalOnProperty(prefix = "orders.processing", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PendingOrderProcessor extends AbstractScheduledJob {

    private final CommandBus commandBus;

    public PendingOrderProcessor(CommandBus commandBus) {
        this.commandBus = commandBus;
    }

    @Scheduled(fixedRateString = "${orders.processing.interval:PT5M}",
               initialDelayString = "${orders.processing.initial-delay:PT1M}")
    public void processPendingOrders() {
        execute();
    }

    @Override
    protected String jobName() {
        return "promote-pending-orders";
    }

    @Override
    protected int doRun() {
        return commandBus.dispatch(new PromotePendingOrdersCommand());
    }
}
