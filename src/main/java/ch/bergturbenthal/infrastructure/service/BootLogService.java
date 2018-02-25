package ch.bergturbenthal.infrastructure.service;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import ch.bergturbenthal.infrastructure.event.BootAction;
import ch.bergturbenthal.infrastructure.model.MacAddress;
import lombok.NonNull;
import lombok.Value;

public interface BootLogService {
    @Value
    public static class BootLogEntry {
        @NonNull
        private Instant              timestamp;
        @NonNull
        private MacAddress           macAddress;
        @NonNull
        private Optional<UUID>       uuid;
        @NonNull
        private Optional<BootAction> configuration;
    }

    public Collection<BootLogEntry> readLastNEntries(int maxEntryCount);

}
