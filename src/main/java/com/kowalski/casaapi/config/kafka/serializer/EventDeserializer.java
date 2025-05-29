package com.kowalski.casaapi.config.kafka.serializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kowalski.casaapi.config.kafka.event.Event;
import org.apache.kafka.common.serialization.Deserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.Map;

public class EventDeserializer implements Deserializer<Event> {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JsonDeserializer<Event> jsonDeserializer;

    public EventDeserializer() {
        this.jsonDeserializer = new JsonDeserializer<>(Event.class);
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        jsonDeserializer.configure(configs, isKey);
    }

    @Override
    public Event deserialize(String topic, byte[] data) {
        return jsonDeserializer.deserialize(topic, data);
    }

    @Override
    public void close() {
        jsonDeserializer.close();
    }
} 