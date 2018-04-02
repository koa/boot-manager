package ch.bergturbenthal.infrastructure.event;

import lombok.NonNull;
import lombok.Value;

@Value
public class SetMachinePropertyEvent implements Event {
    @NonNull
    private String machineName;
    @NonNull
    private String property;
    @NonNull
    private String value;
}
