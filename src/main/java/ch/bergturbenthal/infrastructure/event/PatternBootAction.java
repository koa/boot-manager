package ch.bergturbenthal.infrastructure.event;

import lombok.Value;

@Value
public class PatternBootAction implements BootAction {
    private String patternName;
}
