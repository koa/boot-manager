package ch.bergturbenthal.infrastructure.event;

import lombok.Value;

@Value
public class RenameMachineEvent implements Event {
    private String oldMachineName;
    private String newMachineName;
}
