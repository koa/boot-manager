package ch.bergturbenthal.infrastructure.service;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import ch.bergturbenthal.infrastructure.model.MacAddress;
import reactor.core.Disposable;

public interface MachineService {
    Set<MacAddress> listFreeMacs();

    public Optional<String> processBoot(Optional<UUID> uuid, MacAddress macAddress);

    Disposable registerForUpdates(Runnable run);

}
