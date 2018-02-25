package ch.bergturbenthal.infrastructure.event;

import lombok.Value;

@Value
public class SetDefaultBootConfigurationEvent implements Event {
    private BootAction bootAction;
}
