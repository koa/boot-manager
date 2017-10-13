package ch.bergturbenthal.infrastructure.event;

import java.util.UUID;

import lombok.Value;

@Value
public class UpdateMachinePatternEvent implements Event {
    private UUID         patternId;
    private String       machineName;
    private String       machinePattern;
    private PatternScope scope;
}
