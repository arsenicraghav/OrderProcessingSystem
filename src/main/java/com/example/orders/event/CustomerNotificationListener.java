package com.example.orders.event;

import com.example.orders.domain.event.OrderCreatedEvent;
import com.example.orders.domain.event.OrderStatusChangedEvent;
import com.example.orders.notification.NotificationChannel;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/** <b>Observer</b>: tells the customer about their order through the configured channel. */
@Component
public class CustomerNotificationListener {

    private final NotificationChannel channel;

    public CustomerNotificationListener(NotificationChannel channel) {
        this.channel = channel;
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void on(OrderCreatedEvent e) {
        channel.send(e.customerId(), "Order %d received, total %s".formatted(e.orderId(), e.totalAmount()));
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void on(OrderStatusChangedEvent e) {
        channel.send(e.customerId(), "Order %d is now %s".formatted(e.orderId(), e.to()));
    }
}
