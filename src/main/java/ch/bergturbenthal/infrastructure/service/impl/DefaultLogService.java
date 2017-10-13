package ch.bergturbenthal.infrastructure.service.impl;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import ch.bergturbenthal.infrastructure.db.model.Log;
import ch.bergturbenthal.infrastructure.db.repository.LogRepository;
import ch.bergturbenthal.infrastructure.event.Event;
import ch.bergturbenthal.infrastructure.service.LogService;
import lombok.Cleanup;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Slf4j
@Service
public class DefaultLogService implements LogService {
    @Value
    private static class DefaultEventData implements EventData {
        private UUID    id;
        private Event   event;
        private Instant timestamp;
    }

    private final LogRepository                                    logRepository;
    private final ObjectReader                                     eventReader;
    private final ObjectWriter                                     eventWriter;

    private final Collection<Consumer<Optional<DefaultEventData>>> logListerners = Collections.synchronizedList(new ArrayList<>());
    private final PlatformTransactionManager                       platformTransactionManager;

    @Autowired
    public DefaultLogService(final LogRepository logRepository, final ObjectMapper objectMapper,
            final PlatformTransactionManager platformTransactionManager) {
        this.logRepository = logRepository;
        this.platformTransactionManager = platformTransactionManager;
        eventReader = objectMapper.readerFor(Event.class);
        eventWriter = objectMapper.writerFor(Event.class);
    }

    @Override
    public void appendEvent(final Event event) {
        try {
            final UUID id = UUID.randomUUID();
            final Instant timestamp = Instant.now();
            final String data = eventWriter.writeValueAsString(event);
            final Log newLogEntry = new Log(id, timestamp, data);
            final Optional<DefaultEventData> publishEvent = Optional.of(new DefaultEventData(id, event, timestamp));
            logListerners.forEach(c -> c.accept(publishEvent));
            logRepository.save(newLogEntry);
        } catch (final JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot encode event " + event, e);
        }

    }

    @Override
    public Flux<EventData> streamEvents() {
        return Flux.push(sink -> {
            final AtomicBoolean processingStoredEntries = new AtomicBoolean(true);
            final Map<UUID, EventData> bufferedLogEntries = new ConcurrentHashMap<>();
            final Consumer<Optional<DefaultEventData>> realtimeConsumer = new Consumer<Optional<DefaultEventData>>() {

                @Override
                public void accept(final Optional<DefaultEventData> t) {
                    if (sink.isCancelled()) {
                        logListerners.remove(this);
                        return;
                    }
                    if (processingStoredEntries.get()) {
                        if (t.isPresent()) {
                            bufferedLogEntries.put(t.get().getId(), t.get());
                        }
                    } else {
                        if (!bufferedLogEntries.isEmpty()) {
                            new HashMap<>(bufferedLogEntries).entrySet().stream().sorted(Comparator.comparing(e -> e.getValue().getTimestamp()))
                                    .map(e -> e.getKey()).forEachOrdered(key -> {
                                        final EventData bufferedEntry = bufferedLogEntries.remove(key);
                                        if (!sink.isCancelled()) {
                                            sink.next(bufferedEntry);
                                        }
                                    });
                            ;
                        }
                        if (t.isPresent()) {
                            sink.next(t.get());
                        }

                    }
                }

            };
            logListerners.add(realtimeConsumer);
            new TransactionTemplate(platformTransactionManager).execute(status -> {
                @Cleanup
                final Stream<Log> storedStream = logRepository.readAllOrdered();
                storedStream.forEach(logEntry -> {
                    if (sink.isCancelled()) {
                        return;
                    }
                    try {
                        final Event event = eventReader.readValue(logEntry.getData());
                        final Instant timestamp = logEntry.getTimestamp();
                        sink.next(new DefaultEventData(logEntry.getId(), event, timestamp));
                    } catch (final IOException e) {
                        log.error("Cannot decode entry " + logEntry.getId(), e);
                    }
                });
                return null;
            });
            processingStoredEntries.set(false);
            realtimeConsumer.accept(Optional.empty());
        });
    }

}
