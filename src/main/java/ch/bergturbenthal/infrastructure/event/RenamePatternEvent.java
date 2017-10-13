package ch.bergturbenthal.infrastructure.event;

import lombok.Value;

@Value
public class RenamePatternEvent implements Event {
    private String oldPatternName;
    private String newPatternName;
}
