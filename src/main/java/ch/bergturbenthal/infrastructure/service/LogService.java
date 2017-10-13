package ch.bergturbenthal.infrastructure.service;

import java.time.Instant;

import ch.bergturbenthal.infrastructure.event.Event;
import reactor.core.publisher.Flux;

public interface LogService {
    public interface EventData {
        Event getEvent();

        Instant getTimestamp();
    }

    void appendEvent(Event event);

    Flux<EventData> streamEvents();
}
