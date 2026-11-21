package com.example.orders;

import com.example.orders.support.DevContainersConfiguration;
import org.springframework.boot.SpringApplication;

/**
 * Runs the real application against a PostgreSQL container, with no local database install:
 * {@code mvn spring-boot:test-run} (or run this class from the IDE). Requires Docker.
 */
public class TestOrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.from(OrderServiceApplication::main)
                .with(DevContainersConfiguration.class)
                .run(args);
    }
}
