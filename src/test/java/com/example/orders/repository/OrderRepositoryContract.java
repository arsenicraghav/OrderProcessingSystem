package com.example.orders.repository;

import com.example.orders.domain.Order;
import com.example.orders.domain.OrderStatus;
import com.example.orders.service.query.OrderSearchCriteria;
import com.example.orders.service.query.OrderSpecifications;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Test contract: runs on H2 (*H2Test, mvn test) and PostgreSQL (*PostgresIT, mvn verify).
/** Persistence layer: migrations, mappings, queries, constraints and locking. */
@SpringBootTest(properties = "orders.processing.enabled=false")
public abstract class OrderRepositoryContract {

    @Autowired protected OrderRepository repository;
    @Autowired protected JdbcTemplate jdbc;
    @Autowired protected Flyway flyway;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    protected Order save(String customer) {
        return repository.save(Order.builder()
                .customerId(customer)
                .item("sku-1", "Keyboard", 2, new BigDecimal("49.99"))
                .item("sku-2", "Mouse", 1, new BigDecimal("20.02"))
                .build());
    }

    @Test
    void flywayAppliedTheMigration() {
        // Ask Flyway itself: portable across databases (H2 folds unquoted names to upper case,
        // PostgreSQL to lower case, so querying flyway_schema_history directly isn't).
        MigrationInfo current = flyway.info().current();
        assertThat(current).isNotNull();
        assertThat(current.getVersion().getVersion()).isEqualTo("1");
        assertThat(current.getState().isApplied()).isTrue();
        assertThat(current.getState().isFailed()).isFalse();
        assertThat(flyway.info().pending()).isEmpty();
    }

    @Test
    void savesAndLoadsOrderWithItemsInOneQuery() {
        Order saved = save("c-1");
        Order loaded = repository.findWithItemsById(saved.getId()).orElseThrow();
        assertThat(loaded.getItems()).hasSize(2);   // fetched by the entity graph (entity is detached here)
        assertThat(loaded.getTotalAmount()).isEqualByComparingTo("120.00");
        assertThat(loaded.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(loaded.getCreatedAt()).isNotNull().isBeforeOrEqualTo(Instant.now());
    }

    @Test
    void specificationsCombineFilters() {
        save("c-1");
        save("c-2");
        Order other = save("c-2");
        jdbc.update("update orders set status = 'SHIPPED' where id = ?", other.getId());

        assertThat(repository.findAll(OrderSpecifications.matching(OrderSearchCriteria.all()), PageRequest.of(0, 10))
                .getTotalElements()).isEqualTo(3);
        assertThat(repository.findAll(OrderSpecifications.matching(new OrderSearchCriteria(null, "c-2")), PageRequest.of(0, 10))
                .getTotalElements()).isEqualTo(2);
        assertThat(repository.findAll(OrderSpecifications.matching(new OrderSearchCriteria(OrderStatus.PENDING, "c-2")), PageRequest.of(0, 10))
                .getTotalElements()).isEqualTo(1);
    }

    @Test
    void bulkTransitionTouchesOnlyMatchingRowsAndBumpsVersion() {
        Order pending = save("c-1");
        Order shipped = save("c-1");
        jdbc.update("update orders set status = 'SHIPPED' where id = ?", shipped.getId());

        int changed = repository.bulkTransition(OrderStatus.PENDING, OrderStatus.PROCESSING, Instant.now());

        assertThat(changed).isEqualTo(1);
        Order reloaded = repository.findById(pending.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(OrderStatus.PROCESSING);
        assertThat(reloaded.getVersion()).isEqualTo(pending.getVersion() + 1);
        assertThat(repository.findById(shipped.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.SHIPPED);
    }

    @Test
    void staleWriteIsRejectedByOptimisticLock() {
        Order stale = repository.findWithItemsById(save("c-1").getId()).orElseThrow();
        repository.bulkTransition(OrderStatus.PENDING, OrderStatus.PROCESSING, Instant.now());

        stale.cancel();   // stale copy still believes it's PENDING
        assertThatThrownBy(() -> repository.save(stale)).isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    void databaseConstraintsAreEnforced() {
        Order order = save("c-1");
        assertThatThrownBy(() -> jdbc.update("update orders set status = 'LOST' where id = ?", order.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(
                "insert into order_items (order_id, product_id, product_name, quantity, unit_price) values (?, 'p', 'x', 0, 1)",
                order.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(
                "insert into order_items (order_id, product_id, product_name, quantity, unit_price) values (999999, 'p', 'x', 1, 1)"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deletingAnOrderCascadesToItems() {
        Order order = save("c-1");
        jdbc.update("delete from orders where id = ?", order.getId());
        Integer items = jdbc.queryForObject("select count(*) from order_items where order_id = ?", Integer.class, order.getId());
        assertThat(items).isZero();
    }
}
