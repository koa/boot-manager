package ch.bergturbenthal.infrastructure.event;

import java.util.Optional;
import java.util.UUID;

import ch.bergturbenthal.infrastructure.model.MacAddress;
import lombok.NonNull;
import lombok.Value;

@Value
public class ServerRequestEvent implements Event {
    @NonNull
    private MacAddress       macAddress;
    private Optional<UUID>   machineUuid;
    private Optional<String> selectedPattern;
}
