package com.example.orders.api;

import com.example.orders.support.TestBrokers;
import com.example.orders.support.TestDatabases;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** {@link OrderApiContract} on PostgreSQL + Artemis 2.57.0 (Testcontainers). Runs with {@code mvn verify}; needs Docker. */
class OrderApiPostgresIT extends OrderApiContract {

    @DynamicPropertySource
    static void infrastructure(DynamicPropertyRegistry registry) {
        TestDatabases.postgres(registry);
        TestBrokers.artemis(registry);
    }
}
