package com.example.orders.config;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseUrlEnvironmentPostProcessorTest {

    @Test
    void convertsRenderStyleUrl() {
        Map<String, Object> p = DatabaseUrlEnvironmentPostProcessor.toDataSourceProperties(
                "postgresql://orders:s3cret@dpg-abc-a:5432/orders_x1y2");
        assertThat(p).containsEntry("spring.datasource.url", "jdbc:postgresql://dpg-abc-a:5432/orders_x1y2")
                .containsEntry("spring.datasource.username", "orders")
                .containsEntry("spring.datasource.password", "s3cret");
    }

    @Test
    void keepsQueryAndDefaultsPortAndDecodesPassword() {
        Map<String, Object> p = DatabaseUrlEnvironmentPostProcessor.toDataSourceProperties(
                "postgres://u:p%40ss%3Aword@ep-cool.neon.tech/neondb?sslmode=require");
        assertThat(p).containsEntry("spring.datasource.url",
                        "jdbc:postgresql://ep-cool.neon.tech:5432/neondb?sslmode=require")
                .containsEntry("spring.datasource.password", "p@ss:word");
    }

    @Test
    void ignoresNonPostgresUrls() {
        assertThat(DatabaseUrlEnvironmentPostProcessor.toDataSourceProperties("mysql://u:p@h/db")).isEmpty();
    }
}
