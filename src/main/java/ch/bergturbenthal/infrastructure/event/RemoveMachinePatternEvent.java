package ch.bergturbenthal.infrastructure.event;

import java.util.UUID;

import lombok.Value;

@Value
public class RemoveMachinePatternEvent implements Event {
    private UUID patternId;
}
