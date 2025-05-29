package com.kowalski.casaapi.config.kafka.event;

import java.io.Serializable;

public interface Event extends Serializable {
    String getEventType();
} 