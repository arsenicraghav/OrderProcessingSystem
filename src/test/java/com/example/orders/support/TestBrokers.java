package com.example.orders.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.activemq.ArtemisContainer;
import org.testcontainers.utility.DockerImageName;

/** The two message brokers the test contracts run on; pair with {@link TestDatabases}. */
public final class TestBrokers {

    /** Artemis 2.57.0. The image moved to apache/artemis when Artemis became its own Apache project. */
    public static final DockerImageName ARTEMIS_IMAGE = DockerImageName.parse("apache/artemis:2.57.0")
            .asCompatibleSubstituteFor("apache/activemq-artemis");

    private TestBrokers() {}

    /** In-process Artemis (one per Spring context, non-persistent). No Docker needed. */
    public static void embedded(DynamicPropertyRegistry registry) {
        registry.add("spring.artemis.mode", () -> "embedded");
    }

    /** Real Artemis 2.57.0 broker in Docker; one container shared by all tests, started lazily. */
    public static void artemis(DynamicPropertyRegistry registry) {
        ArtemisContainer broker = ArtemisHolder.CONTAINER;
        registry.add("spring.artemis.mode", () -> "native");
        registry.add("spring.artemis.broker-url", broker::getBrokerUrl);
        registry.add("spring.artemis.user", broker::getUser);
        registry.add("spring.artemis.password", broker::getPassword);
    }

    private static final class ArtemisHolder {
        static final ArtemisContainer CONTAINER = start();

        private static ArtemisContainer start() {
            ArtemisContainer container = new ArtemisContainer(ARTEMIS_IMAGE);
            container.start();
            return container;
        }
    }
}
