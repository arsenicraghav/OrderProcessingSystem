package com.example.orders.scheduler;

import com.example.orders.domain.InvalidOrderStateException;
import com.example.orders.domain.Order;
import com.example.orders.domain.OrderStatus;
import com.example.orders.domain.event.PendingOrdersPromotedEvent;
import com.example.orders.repository.OrderRepository;
import com.example.orders.service.command.*;
import com.example.orders.service.query.OrderQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Test contract: runs on H2 (*H2Test, mvn test) and PostgreSQL (*PostgresIT, mvn verify).
/** Job logic is triggered directly; the real schedule is pushed out with a long initial delay. */
@SpringBootTest(properties = "orders.processing.initial-delay=PT1H")
@RecordApplicationEvents
public abstract class PendingOrderProcessorContract {

    @Autowired PendingOrderProcessor processor;
    @Autowired CommandBus commandBus;
    @Autowired OrderQueryService queries;
    @Autowired OrderRepository repository;
    @Autowired ApplicationEvents events;
    @Value("${orders.processing.interval}") Duration interval;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    private Order newOrder() {
        return commandBus.dispatch(new CreateOrderCommand("c-1",
                List.of(new CreateOrderCommand.Line("p-1", "Widget", 1, new BigDecimal("5.00")))));
    }

    private OrderStatus statusOf(Order o) {
        return queries.getOrder(o.getId()).getStatus();
    }

    @Test
    void defaultIntervalIsFiveMinutes() throws Exception {
        assertThat(interval).isEqualTo(Duration.ofMinutes(5));
        Scheduled scheduled = PendingOrderProcessor.class
                .getMethod("processPendingOrders").getAnnotation(Scheduled.class);
        assertThat(scheduled.fixedRateString()).contains("orders.processing.interval");
    }

    @Test
    void promotesOnlyPendingOrdersAndPublishesOneEvent() {
        Order p1 = newOrder();
        Order p2 = newOrder();
        Order cancelled = newOrder();
        commandBus.dispatch(new CancelOrderCommand(cancelled.getId()));
        Order shipped = newOrder();
        commandBus.dispatch(new ChangeOrderStatusCommand(shipped.getId(), OrderStatus.PROCESSING));
        commandBus.dispatch(new ChangeOrderStatusCommand(shipped.getId(), OrderStatus.SHIPPED));

        processor.processPendingOrders();

        assertThat(statusOf(p1)).isEqualTo(OrderStatus.PROCESSING);
        assertThat(statusOf(p2)).isEqualTo(OrderStatus.PROCESSING);
        assertThat(statusOf(cancelled)).isEqualTo(OrderStatus.CANCELLED);
        assertThat(statusOf(shipped)).isEqualTo(OrderStatus.SHIPPED);
        assertThat(events.stream(PendingOrdersPromotedEvent.class))
                .singleElement().extracting(PendingOrdersPromotedEvent::count).isEqualTo(2);
    }

    @Test
    void jobIsIdempotent() {
        newOrder();
        assertThat(commandBus.dispatch(new PromotePendingOrdersCommand())).isEqualTo(1);
        assertThat(commandBus.dispatch(new PromotePendingOrdersCommand())).isZero();
    }

    @Test
    void orderCannotBeCancelledAfterJobRuns() {
        Order o = newOrder();
        processor.processPendingOrders();
        assertThatThrownBy(() -> commandBus.dispatch(new CancelOrderCommand(o.getId())))
                .isInstanceOf(InvalidOrderStateException.class);
    }

    @Test
    void staleCancelAfterJobIsRejectedByVersionCheck() {
        Order stale = queries.getOrder(newOrder().getId());   // loaded while PENDING
        processor.processPendingOrders();                      // job promotes it, bumps version
        stale.cancel();                                        // stale copy still thinks it's PENDING
        assertThatThrownBy(() -> repository.save(stale))
                .isInstanceOf(org.springframework.dao.OptimisticLockingFailureException.class);
        assertThat(statusOf(stale)).isEqualTo(OrderStatus.PROCESSING);
    }

    @Test
    void jobBumpsVersionSoStaleWritersAreDetected() {
        Order o = newOrder();
        long before = repository.findById(o.getId()).orElseThrow().getVersion();
        processor.processPendingOrders();
        assertThat(repository.findById(o.getId()).orElseThrow().getVersion()).isEqualTo(before + 1);
    }
}
