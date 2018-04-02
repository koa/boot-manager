package ch.bergturbenthal.infrastructure.event;

import lombok.NonNull;
import lombok.Value;

@Value
public class RemoveMachinePropertyEvent implements Event {
    @NonNull
    private String machineName;
    @NonNull
    private String property;
}
