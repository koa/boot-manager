package ch.bergturbenthal.infrastructure.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import ch.bergturbenthal.infrastructure.event.BootAction;
import ch.bergturbenthal.infrastructure.event.PatternScope;
import ch.bergturbenthal.infrastructure.model.BootContext;
import ch.bergturbenthal.infrastructure.model.MacAddress;
import ch.bergturbenthal.infrastructure.service.BootLogService.BootLogEntry;
import lombok.Builder;
import lombok.Value;

public interface MachineService {
    @Value
    static class BootConfigurationEntry {
        private UUID         uuid;
        private PatternScope scope;
        private BootAction   configuration;
    }

    @Value
    @Builder
    static class ServerData {
        private String                       name;
        private UUID                         uuid;
        private Collection<MacAddress>       macs;
        private List<BootLogEntry>           bootHistory;
        private List<BootConfigurationEntry> bootConfiguration;
        private Map<String, String>          properties;
    }

    Optional<BootAction> evaluateNextBootConfiguration(String machineName);

    Optional<ServerData> findServerByName(String serverName);

    Set<MacAddress> listFreeMacs();

    Map<UUID, Collection<MacAddress>> listFreeUUIDs();

    List<ServerData> listServers();

    Collection<MacAddress> macsOfUUID(UUID uuid);

    Optional<BootContext> processBoot(Optional<UUID> uuid, MacAddress macAddress);

}
