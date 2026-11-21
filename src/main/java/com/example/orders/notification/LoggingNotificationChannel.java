package com.example.orders.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Default channel: writes the notification to the log. */
@Component
public class LoggingNotificationChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationChannel.class);

    @Override
    public void send(String customerId, String message) {
        log.info("[notify customer {}] {}", customerId, message);
    }
}
