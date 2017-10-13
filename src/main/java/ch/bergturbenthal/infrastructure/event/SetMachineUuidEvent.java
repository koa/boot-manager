package ch.bergturbenthal.infrastructure.event;

import java.util.UUID;

import lombok.Value;

@Value
public class SetMachineUuidEvent implements Event {
    private String machineName;
    private UUID   machineUuid;
}
