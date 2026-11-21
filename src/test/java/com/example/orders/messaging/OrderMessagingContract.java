package com.example.orders.messaging;

import com.example.orders.domain.Order;
import com.example.orders.domain.OrderStatus;
import com.example.orders.repository.OrderRepository;
import com.example.orders.service.command.ChangeOrderStatusCommand;
import com.example.orders.service.command.CommandBus;
import com.example.orders.service.command.CreateOrderCommand;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.core.JmsTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

// Test contract: runs on H2 + embedded Artemis (*H2Test, mvn test)
// and PostgreSQL + Artemis 2.57.0 container (*PostgresIT, mvn verify).
/** Outbound order events and inbound status updates over JMS. */
@SpringBootTest(properties = "orders.processing.enabled=false")
public abstract class OrderMessagingContract {

    private static final Duration WAIT = Duration.ofSeconds(10);

    @Autowired CommandBus commandBus;
    @Autowired OrderRepository repository;
    @Autowired ConnectionFactory connectionFactory;
    @Autowired ObjectMapper objectMapper;
    @Value("${orders.messaging.events-queue}") String eventsQueue;
    @Value("${orders.messaging.status-updates-queue}") String statusUpdatesQueue;

    private JmsTemplate jms;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        jms = new JmsTemplate(connectionFactory);
        jms.setReceiveTimeout(200);
        while (jms.receive(eventsQueue) != null) {
            // drain events left over from other tests
        }
    }

    private Order newOrder() {
        return commandBus.dispatch(new CreateOrderCommand("c-1",
                List.of(new CreateOrderCommand.Line("sku-1", "Keyboard", 1, new BigDecimal("49.99")))));
    }

    /** Reads the events queue until the expected event for {@code orderId} arrives. */
    private JsonNode awaitEvent(String eventType, Long orderId) throws Exception {
        long deadline = System.nanoTime() + WAIT.toNanos();
        while (System.nanoTime() < deadline) {
            Message message = jms.receive(eventsQueue);
            if (message == null) continue;
            JsonNode json = objectMapper.readTree(((TextMessage) message).getText());
            if (eventType.equals(json.path("eventType").asText()) && orderId.equals(json.path("orderId").asLong())) {
                assertThat(message.getStringProperty(JmsOrderEventPublisher.EVENT_TYPE_PROPERTY)).isEqualTo(eventType);
                assertThat(message.getLongProperty(JmsOrderEventPublisher.ORDER_ID_PROPERTY)).isEqualTo(orderId);
                return json;
            }
        }
        return fail("No %s event for order %d within %s", eventType, orderId, WAIT);
    }

    private void awaitStatus(Long orderId, OrderStatus expected) throws InterruptedException {
        long deadline = System.nanoTime() + WAIT.toNanos();
        OrderStatus status = null;
        while (System.nanoTime() < deadline) {
            status = repository.findById(orderId).orElseThrow().getStatus();
            if (status == expected) return;
            Thread.sleep(100);
        }
        fail("Order %d is %s, expected %s", orderId, status, expected);
    }

    private void sendStatusUpdate(String json) {
        jms.convertAndSend(statusUpdatesQueue, json);
    }

    // ---------- outbound ----------

    @Test
    void publishesOrderCreatedEvent() throws Exception {
        Order order = newOrder();
        JsonNode event = awaitEvent(OrderEventMessage.ORDER_CREATED, order.getId());
        assertThat(event.path("schemaVersion").asInt()).isEqualTo(1);
        assertThat(event.path("customerId").asText()).isEqualTo("c-1");
        assertThat(event.path("toStatus").asText()).isEqualTo("PENDING");
        assertThat(event.path("totalAmount").decimalValue()).isEqualByComparingTo("49.99");
        assertThat(event.path("occurredAt").asText()).isNotBlank();
    }

    @Test
    void publishesStatusChangedEvent() throws Exception {
        Order order = newOrder();
        commandBus.dispatch(new ChangeOrderStatusCommand(order.getId(), OrderStatus.PROCESSING));
        JsonNode event = awaitEvent(OrderEventMessage.ORDER_STATUS_CHANGED, order.getId());
        assertThat(event.path("fromStatus").asText()).isEqualTo("PENDING");
        assertThat(event.path("toStatus").asText()).isEqualTo("PROCESSING");
    }

    // ---------- inbound ----------

    @Test
    void appliesStatusUpdateFromQueue() throws Exception {
        Order order = newOrder();
        sendStatusUpdate("{\"orderId\":" + order.getId() + ",\"status\":\"PROCESSING\"}");
        awaitStatus(order.getId(), OrderStatus.PROCESSING);
        awaitEvent(OrderEventMessage.ORDER_STATUS_CHANGED, order.getId());   // and tells the world
    }

    @Test
    void badUpdatesAreDroppedWithoutBlockingTheQueue() throws Exception {
        Order order = newOrder();
        sendStatusUpdate("{not json");                                                        // malformed
        sendStatusUpdate("{\"orderId\":" + order.getId() + ",\"status\":\"DELIVERED\"}");     // illegal jump
        sendStatusUpdate("{\"orderId\":999999,\"status\":\"SHIPPED\"}");                     // unknown order
        sendStatusUpdate("{\"orderId\":" + order.getId() + ",\"status\":\"LOST\"}");          // unknown status
        sendStatusUpdate("{\"orderId\":" + order.getId() + ",\"status\":\"PROCESSING\"}");    // valid

        awaitStatus(order.getId(), OrderStatus.PROCESSING);   // the valid one still got through
    }

    @Test
    void cannotCancelViaQueueOnceProcessing() throws Exception {
        Order order = newOrder();
        commandBus.dispatch(new ChangeOrderStatusCommand(order.getId(), OrderStatus.PROCESSING));
        sendStatusUpdate("{\"orderId\":" + order.getId() + ",\"status\":\"CANCELLED\"}");
        sendStatusUpdate("{\"orderId\":" + order.getId() + ",\"status\":\"SHIPPED\"}");
        awaitStatus(order.getId(), OrderStatus.SHIPPED);   // cancel was rejected by the State rules
    }
}
