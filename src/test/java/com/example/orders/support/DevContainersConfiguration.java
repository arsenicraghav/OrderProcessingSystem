package com.example.orders.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

/** Used by {@link com.example.orders.TestOrderServiceApplication}: a throwaway Postgres for local runs. */
@TestConfiguration(proxyBeanMethods = false)
public class DevContainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(TestDatabases.POSTGRES_IMAGE);
    }
}
