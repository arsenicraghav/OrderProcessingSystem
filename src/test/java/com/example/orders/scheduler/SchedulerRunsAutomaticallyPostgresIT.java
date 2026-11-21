package com.example.orders.scheduler;

import com.example.orders.support.TestBrokers;
import com.example.orders.support.TestDatabases;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** {@link SchedulerRunsAutomaticallyContract} on PostgreSQL + Artemis 2.57.0 (Testcontainers). Runs with {@code mvn verify}; needs Docker. */
class SchedulerRunsAutomaticallyPostgresIT extends SchedulerRunsAutomaticallyContract {

    @DynamicPropertySource
    static void infrastructure(DynamicPropertyRegistry registry) {
        TestDatabases.postgres(registry);
        TestBrokers.artemis(registry);
    }
}
