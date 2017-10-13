package ch.bergturbenthal.infrastructure.event;

import ch.bergturbenthal.infrastructure.model.MacAddress;
import lombok.Value;

@Value
public class AssignMacAddressToMachineEvent implements Event {
    private String     machineName;
    private MacAddress macAddress;
}
