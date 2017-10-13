package ch.bergturbenthal.infrastructure.event;

import ch.bergturbenthal.infrastructure.model.MacAddress;
import lombok.Value;

@Value
public class RemoveMacAddressFromMachineEvent implements Event {
    private String     machineName;
    private MacAddress macAddress;
}
