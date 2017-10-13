package ch.bergturbenthal.infrastructure.event;

import lombok.Value;

@Value
public class RemoveMachineEvent implements Event {
    private String machineName;
}
