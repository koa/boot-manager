package ch.bergturbenthal.infrastructure.event;

import lombok.Value;

@Value
public class UpdatePatternEvent implements Event {
    private String patternName;
    private String patternContent;
}
