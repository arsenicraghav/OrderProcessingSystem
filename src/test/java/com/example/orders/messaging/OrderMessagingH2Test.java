package com.example.orders.messaging;

import com.example.orders.support.TestBrokers;
import com.example.orders.support.TestDatabases;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** {@link OrderMessagingContract} on H2 + embedded Artemis. Runs with {@code mvn test}; no Docker. */
class OrderMessagingH2Test extends OrderMessagingContract {

    @DynamicPropertySource
    static void infrastructure(DynamicPropertyRegistry registry) {
        TestDatabases.h2(registry);
        TestBrokers.embedded(registry);
    }
}
