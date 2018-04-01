package ch.bergturbenthal.infrastructure.model;

import java.util.Optional;

import ch.bergturbenthal.infrastructure.event.BootAction;
import lombok.Value;

@Value
public class BootContext {
    @Value
    public static class ContextData {
        private String machineName;
    }

    private BootAction            action;
    private Optional<ContextData> context;
}
