package ch.bergturbenthal.infrastructure.service;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import ch.bergturbenthal.infrastructure.model.MacAddress;
import lombok.Builder;
import lombok.Value;
import reactor.core.Disposable;

public interface MachineService {
    @Value
    @Builder
    static class ServerData {
        private String                 name;
        private UUID                   uuid;
        private Collection<MacAddress> macs;
        private Instant                lastBootTime;
    }

    Set<MacAddress> listFreeMacs();

    List<ServerData> listServers();

    public Optional<String> processBoot(Optional<UUID> uuid, MacAddress macAddress);

    Disposable registerForUpdates(Runnable run);

}
