package com.example.orders.event;

import com.example.orders.domain.event.OrderCreatedEvent;
import com.example.orders.domain.event.OrderStatusChangedEvent;
import com.example.orders.domain.event.PendingOrdersPromotedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * <b>Observer pattern</b>: reacts to domain events without the publishers knowing it exists.
 * Runs only after the transaction commits, so it never records a change that was rolled back.
 */
@Component
public class OrderAuditListener {

    private static final Logger audit = LoggerFactory.getLogger("ORDER_AUDIT");

    @TransactionalEventListener(fallbackExecution = true)
    public void on(OrderCreatedEvent e) {
        audit.info("order={} customer={} event=CREATED total={}", e.orderId(), e.customerId(), e.totalAmount());
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void on(OrderStatusChangedEvent e) {
        audit.info("order={} customer={} event=STATUS_CHANGED from={} to={}",
                e.orderId(), e.customerId(), e.from(), e.to());
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void on(PendingOrdersPromotedEvent e) {
        audit.info("event=BULK_PROMOTED count={} to=PROCESSING", e.count());
    }
}
