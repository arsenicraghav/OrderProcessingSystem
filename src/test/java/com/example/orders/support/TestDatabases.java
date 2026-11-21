package com.example.orders.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * The two databases the integration-test contracts run on. Concrete test classes pick one in a
 * {@code @DynamicPropertySource} method:
 * <pre>
 *   &#64;DynamicPropertySource static void db(DynamicPropertyRegistry r) { TestDatabases.h2(r); }
 *   &#64;DynamicPropertySource static void db(DynamicPropertyRegistry r) { TestDatabases.postgres(r); }
 * </pre>
 */
public final class TestDatabases {

    public static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:16-alpine");

    private TestDatabases() {}

    /** In-memory H2 (MySQL mode) with the {@code db/migration/h2} schema. No Docker needed. */
    public static void h2(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:orders-it;MODE=MySQL;DB_CLOSE_DELAY=-1");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
    }

    /**
     * Real PostgreSQL 16 in Docker with the production {@code db/migration/postgresql} schema.
     * One container is shared by every PostgreSQL test class (singleton container); it starts
     * lazily on first use, so H2-only runs never touch Docker. Testcontainers removes it at JVM exit.
     */
    public static void postgres(DynamicPropertyRegistry registry) {
        PostgreSQLContainer<?> postgres = PostgresHolder.CONTAINER;
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    /** Lazy holder: the container is created and started only when this class is first loaded. */
    private static final class PostgresHolder {
        static final PostgreSQLContainer<?> CONTAINER = start();

        private static PostgreSQLContainer<?> start() {
            PostgreSQLContainer<?> container = new PostgreSQLContainer<>(POSTGRES_IMAGE)
                    .withDatabaseName("orders")
                    .withUsername("orders")
                    .withPassword("orders");
            container.start();
            return container;
        }
    }
}
