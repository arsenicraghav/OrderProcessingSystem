package com.example.orders.scheduler;

import com.example.orders.domain.Order;
import com.example.orders.domain.OrderStatus;
import com.example.orders.repository.OrderRepository;
import com.example.orders.service.command.CommandBus;
import com.example.orders.service.command.CreateOrderCommand;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Test contract: runs on H2 (*H2Test, mvn test) and PostgreSQL (*PostgresIT, mvn verify).
/** Proves the Spring scheduler actually fires the job (interval shortened for the test). */
@SpringBootTest(properties = {
        "orders.processing.interval=PT0.2S",
        "orders.processing.initial-delay=PT0.2S"
})
@DirtiesContext   // stop the fast scheduler once this class finishes
public abstract class SchedulerRunsAutomaticallyContract {

    @Autowired CommandBus commandBus;
    @Autowired OrderRepository repository;

    @Test
    void pendingOrderIsPromotedBySchedule() throws Exception {
        Order o = commandBus.dispatch(new CreateOrderCommand("c-1",
                List.of(new CreateOrderCommand.Line("p-1", "Widget", 1, new BigDecimal("1.00")))));

        long deadline = System.currentTimeMillis() + 5_000;
        OrderStatus status = OrderStatus.PENDING;
        while (System.currentTimeMillis() < deadline) {
            status = repository.findById(o.getId()).orElseThrow().getStatus();
            if (status == OrderStatus.PROCESSING) break;
            Thread.sleep(100);
        }
        assertThat(status).isEqualTo(OrderStatus.PROCESSING);
    }
}
