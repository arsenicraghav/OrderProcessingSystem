package com.example.orders.repository;

import com.example.orders.support.TestBrokers;
import com.example.orders.support.TestDatabases;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link OrderRepositoryContract} on PostgreSQL + Artemis 2.57.0 (Testcontainers), plus PostgreSQL-only checks.
 * Runs with {@code mvn verify}; needs Docker.
 */
class OrderRepositoryPostgresIT extends OrderRepositoryContract {

    @DynamicPropertySource
    static void infrastructure(DynamicPropertyRegistry registry) {
        TestDatabases.postgres(registry);
        TestBrokers.artemis(registry);
    }

    @Test
    void partialIndexForPendingOrdersExists() {
        Integer count = jdbc.queryForObject(
                "select count(*) from pg_indexes where indexname = 'idx_orders_pending'", Integer.class);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void connectedToPostgres() {
        String version = jdbc.queryForObject("select version()", String.class);
        assertThat(version).startsWith("PostgreSQL 16");
    }
}
