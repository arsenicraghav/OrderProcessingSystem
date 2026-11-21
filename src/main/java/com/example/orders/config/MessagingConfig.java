package com.example.orders.config;

import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.core.settings.impl.AddressSettings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jms.artemis.ArtemisConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

/**
 * Artemis settings for the embedded broker. The connection factory, JmsTemplate and
 * {@code @JmsListener} containers are auto-configured by Spring Boot.
 * (Artemis keeps its {@code org.apache.activemq.artemis} Java packages after the groupId move.)
 */
@Configuration
public class MessagingConfig {

    /**
     * Retry a failed inbound message with back-off (1s, 2s, 4s); after 4 delivery attempts Artemis
     * moves it to the {@code DLQ} queue instead of retrying forever. Expired messages go to
     * {@code ExpiryQueue}. Broker memory is capped (default is half the heap) so the app fits small
     * containers; beyond the cap Artemis pages messages to disk.
     */
    @Bean
    ArtemisConfigurationCustomizer redeliveryAndDeadLetterSettings(
            @Value("${orders.messaging.embedded-max-memory:64MB}") DataSize embeddedMaxMemory) {
        return configuration -> configuration
                .setGlobalMaxSize(embeddedMaxMemory.toBytes())
                .addAddressSetting("#", new AddressSettings()
                        .setMaxDeliveryAttempts(4)
                        .setRedeliveryDelay(1_000L)
                        .setRedeliveryMultiplier(2.0)
                        .setDeadLetterAddress(SimpleString.of("DLQ"))
                        .setExpiryAddress(SimpleString.of("ExpiryQueue"))
                        .setAutoCreateDeadLetterResources(true)
                        .setAutoCreateExpiryResources(true));
    }
}
