package com.example.orders.messaging;

import com.example.orders.domain.event.OrderCreatedEvent;
import com.example.orders.domain.event.OrderStatusChangedEvent;
import com.example.orders.domain.event.PendingOrdersPromotedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.TextMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * <b>Observer</b> bridging in-process domain events to ActiveMQ Artemis. Runs only after the database
 * transaction commits, so other systems never hear about a change that was rolled back.
 * A broker outage is logged and never fails the user's request; for guaranteed delivery,
 * replace this with a transactional outbox.
 */
@Component
@ConditionalOnProperty(prefix = "orders.messaging", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JmsOrderEventPublisher {

    public static final String EVENT_TYPE_PROPERTY = "eventType";
    public static final String ORDER_ID_PROPERTY = "orderId";

    private static final Logger log = LoggerFactory.getLogger(JmsOrderEventPublisher.class);

    private final JmsTemplate jmsTemplate;
    private final ObjectMapper objectMapper;
    private final String eventsQueue;

    public JmsOrderEventPublisher(JmsTemplate jmsTemplate, ObjectMapper objectMapper,
                                  @Value("${orders.messaging.events-queue:orders.events}") String eventsQueue) {
        this.jmsTemplate = jmsTemplate;
        this.objectMapper = objectMapper;
        this.eventsQueue = eventsQueue;
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void on(OrderCreatedEvent event) {
        publish(OrderEventMessage.from(event));
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void on(OrderStatusChangedEvent event) {
        publish(OrderEventMessage.from(event));
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void on(PendingOrdersPromotedEvent event) {
        publish(OrderEventMessage.from(event));
    }

    void publish(OrderEventMessage message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            jmsTemplate.send(eventsQueue, session -> {
                TextMessage jms = session.createTextMessage(json);
                // Properties let consumers filter with JMS selectors, e.g. "eventType = 'ORDER_CREATED'"
                jms.setStringProperty(EVENT_TYPE_PROPERTY, message.eventType());
                if (message.orderId() != null) {
                    jms.setLongProperty(ORDER_ID_PROPERTY, message.orderId());
                }
                return jms;
            });
            log.debug("Published {} for order {} to {}", message.eventType(), message.orderId(), eventsQueue);
        } catch (JsonProcessingException | JmsException e) {
            log.error("Could not publish {} for order {} to {}", message.eventType(), message.orderId(), eventsQueue, e);
        }
    }
}
