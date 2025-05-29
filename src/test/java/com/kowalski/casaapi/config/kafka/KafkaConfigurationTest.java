package com.kowalski.casaapi.config.kafka;

import com.kowalski.casaapi.config.kafka.event.Event;
import com.kowalski.casaapi.config.kafka.serializer.EventDeserializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.IsolationLevel;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaConfigurationTest {

    private KafkaConfiguration kafkaConfiguration;
    private static final String BOOTSTRAP_ADDRESS = "localhost:9092";
    private static final String GROUP_ID = "test-group";
    private static final String INITIAL_INTERVAL = "500";
    private static final String MAX_ATTEMPTS = "15";

    @BeforeEach
    void setUp() {
        kafkaConfiguration = new KafkaConfiguration();
        ReflectionTestUtils.setField(kafkaConfiguration, "bootstrapAddress", BOOTSTRAP_ADDRESS);
        ReflectionTestUtils.setField(kafkaConfiguration, "groupId", GROUP_ID);
        ReflectionTestUtils.setField(kafkaConfiguration, "initialInterval", INITIAL_INTERVAL);
        ReflectionTestUtils.setField(kafkaConfiguration, "maxAttempts", MAX_ATTEMPTS);
    }

    @Test
    @DisplayName("Deve criar ProducerFactory com configurações corretas")
    void producerFactory_ConfiguracoesCorretas() {
        // Act
        ProducerFactory<String, Event> factory = kafkaConfiguration.producerFactory();

        // Assert
        assertThat(factory).isNotNull();
    }

    @Test
    @DisplayName("Deve criar KafkaTemplate com configurações corretas")
    void kafkaTemplate_ConfiguracoesCorretas() {
        // Act
        KafkaTemplate<String, Event> template = kafkaConfiguration.kafkaTemplate();

        // Assert
        assertThat(template).isNotNull();
    }

    @Test
    @DisplayName("Deve criar configurações do consumer corretamente")
    void consumerConfigs_ConfiguracoesCorretas() {
        // Act
        Map<String, Object> configs = kafkaConfiguration.consumerConfigs();

        // Assert
        assertThat(configs)
            .containsEntry(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_ADDRESS)
            .containsEntry(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID)
            .containsEntry(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false")
            .containsEntry(ConsumerConfig.ISOLATION_LEVEL_CONFIG, IsolationLevel.READ_COMMITTED.toString().toLowerCase(Locale.ROOT))
            .containsEntry(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class)
            .containsEntry(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, EventDeserializer.class);
    }

    @Test
    @DisplayName("Deve criar ConsumerFactory com configurações corretas")
    void consumerFactory_ConfiguracoesCorretas() {
        // Act
        ConsumerFactory<String, Event> factory = kafkaConfiguration.consumerFactory();

        // Assert
        assertThat(factory).isNotNull();
    }

    @Test
    @DisplayName("Deve criar KafkaListenerContainerFactory com configurações corretas")
    void kafkaListenerContainerFactory_ConfiguracoesCorretas() {
        // Act
        var factory = kafkaConfiguration.kafkaListenerContainerFactory();

        // Assert
        assertThat(factory).isNotNull();
    }
} 