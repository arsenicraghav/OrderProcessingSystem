package com.example.orders.notification;

/**
 * <b>Strategy</b> for how customers are told about their orders. Swap the bean for an
 * email/SMS implementation without touching the event listeners.
 */
public interface NotificationChannel {

    void send(String customerId, String message);
}
