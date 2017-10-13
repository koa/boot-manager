package ch.bergturbenthal.infrastructure.event;

import lombok.Value;

@Value
public class RemoveMachineUUIDEvent implements Event {
    private String machineName;
}
