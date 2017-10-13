package ch.bergturbenthal.infrastructure.event;

import lombok.Value;

@Value
public class RemovePatternEvent implements Event {
    private String patternName;
}
