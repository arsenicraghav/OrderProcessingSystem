package com.example.orders.messaging;

import com.example.orders.domain.InvalidOrderStateException;
import com.example.orders.domain.OrderNotFoundException;
import com.example.orders.domain.OrderValidationException;
import com.example.orders.service.command.ChangeOrderStatusCommand;
import com.example.orders.service.command.CommandBus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

/**
 * Consumes status updates sent by other systems (e.g. the warehouse marks an order SHIPPED) and
 * applies them through the same {@link CommandBus} as the REST API, so the same State rules apply.
 * <ul>
 *   <li><b>Permanent</b> failures (bad JSON, unknown order, illegal transition) are logged and
 *       acknowledged: retrying would never succeed and would block the queue.</li>
 *   <li><b>Transient</b> failures (e.g. a concurrent update) are rethrown: the JMS session rolls
 *       back, Artemis redelivers with back-off, then moves the message to the dead-letter queue
 *       {@code DLQ} (see {@code MessagingConfig}).</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(prefix = "orders.messaging", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OrderStatusUpdateListener {

    private static final Logger log = LoggerFactory.getLogger(OrderStatusUpdateListener.class);

    private final CommandBus commandBus;
    private final ObjectMapper objectMapper;

    public OrderStatusUpdateListener(CommandBus commandBus, ObjectMapper objectMapper) {
        this.commandBus = commandBus;
        this.objectMapper = objectMapper;
    }

    @JmsListener(destination = "${orders.messaging.status-updates-queue:orders.status-updates}")
    public void onMessage(String body) {
        OrderStatusUpdateMessage update;
        try {
            update = objectMapper.readValue(body, OrderStatusUpdateMessage.class);
        } catch (JsonProcessingException e) {
            log.warn("Dropping malformed status update: {}", abbreviate(body));
            return;
        }
        if (update.orderId() == null || update.status() == null) {
            log.warn("Dropping status update without orderId/status: {}", abbreviate(body));
            return;
        }
        try {
            commandBus.dispatch(new ChangeOrderStatusCommand(update.orderId(), update.status()));
            log.info("Applied status update: order {} -> {}", update.orderId(), update.status());
        } catch (OrderNotFoundException | InvalidOrderStateException | OrderValidationException e) {
            log.warn("Rejected status update for order {} -> {}: {}", update.orderId(), update.status(), e.getMessage());
        }
    }

    private static String abbreviate(String s) {
        return s == null || s.length() <= 200 ? s : s.substring(0, 200) + "…";
    }
}
