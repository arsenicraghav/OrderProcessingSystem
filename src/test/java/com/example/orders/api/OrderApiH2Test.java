package com.example.orders.api;

import com.example.orders.support.TestBrokers;
import com.example.orders.support.TestDatabases;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** {@link OrderApiContract} on H2 + embedded Artemis. Runs with {@code mvn test}; no Docker. */
class OrderApiH2Test extends OrderApiContract {

    @DynamicPropertySource
    static void infrastructure(DynamicPropertyRegistry registry) {
        TestDatabases.h2(registry);
        TestBrokers.embedded(registry);
    }
}
