package com.example.orders.repository;

import com.example.orders.support.TestBrokers;
import com.example.orders.support.TestDatabases;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** {@link OrderRepositoryContract} on H2 + embedded Artemis. Runs with {@code mvn test}; no Docker. */
class OrderRepositoryH2Test extends OrderRepositoryContract {

    @DynamicPropertySource
    static void infrastructure(DynamicPropertyRegistry registry) {
        TestDatabases.h2(registry);
        TestBrokers.embedded(registry);
    }
}
