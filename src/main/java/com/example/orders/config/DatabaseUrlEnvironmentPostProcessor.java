package com.example.orders.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * <b>Adapter pattern</b>: hosting platforms (Render, Neon, Supabase, Railway, Heroku) hand out a
 * {@code DATABASE_URL} like {@code postgresql://user:pass@host:5432/db?sslmode=require}, but JDBC
 * needs {@code jdbc:postgresql://host:5432/db?sslmode=require} plus separate credentials.
 * This converts one into the other at startup. An explicit {@code SPRING_DATASOURCE_URL} wins.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    static final String SOURCE_NAME = "databaseUrlAdapter";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String databaseUrl = environment.getProperty("DATABASE_URL");
        if (databaseUrl == null || databaseUrl.isBlank() || environment.getProperty("SPRING_DATASOURCE_URL") != null) {
            return;
        }
        Map<String, Object> props = toDataSourceProperties(databaseUrl.trim());
        if (!props.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource(SOURCE_NAME, props));
        }
    }

    /** Returns spring.datasource.* properties, or an empty map if the URL isn't a postgres URL. */
    static Map<String, Object> toDataSourceProperties(String databaseUrl) {
        URI uri = URI.create(databaseUrl);
        String scheme = uri.getScheme();
        if (!"postgres".equals(scheme) && !"postgresql".equals(scheme)) {
            return Map.of();
        }
        if (uri.getHost() == null) {
            throw new IllegalArgumentException("DATABASE_URL has no host");
        }
        int port = uri.getPort() == -1 ? 5432 : uri.getPort();
        StringBuilder jdbc = new StringBuilder("jdbc:postgresql://")
                .append(uri.getHost()).append(':').append(port)
                .append(uri.getRawPath() == null || uri.getRawPath().isEmpty() ? "/" : uri.getRawPath());
        if (uri.getRawQuery() != null) {
            jdbc.append('?').append(uri.getRawQuery());
        }

        Map<String, Object> props = new LinkedHashMap<>();
        props.put("spring.datasource.url", jdbc.toString());
        String userInfo = uri.getRawUserInfo();
        if (userInfo != null) {
            int colon = userInfo.indexOf(':');
            String user = colon < 0 ? userInfo : userInfo.substring(0, colon);
            props.put("spring.datasource.username", decode(user));
            if (colon >= 0) {
                props.put("spring.datasource.password", decode(userInfo.substring(colon + 1)));
            }
        }
        return props;
    }

    private static String decode(String s) {
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;   // after application.yml has been loaded
    }
}
