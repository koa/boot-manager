package ch.bergturbenthal.infrastructure.service;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import ch.bergturbenthal.infrastructure.event.PatternScope;
import ch.bergturbenthal.infrastructure.model.MacAddress;
import lombok.Builder;
import lombok.Value;

public interface MachineService {
    @Value
    static class BootConfigurationEntry {
        private UUID         uuid;
        private PatternScope scope;
        private String       configuration;
    }

    @Value
    @Builder
    static class ServerData {
        private String                       name;
        private UUID                         uuid;
        private Collection<MacAddress>       macs;
        private Optional<Instant>            lastBootTime;
        private List<BootConfigurationEntry> bootConfiguration;
    }

    Set<MacAddress> listFreeMacs();

    List<ServerData> listServers();

    public Optional<String> processBoot(Optional<UUID> uuid, MacAddress macAddress);

}
