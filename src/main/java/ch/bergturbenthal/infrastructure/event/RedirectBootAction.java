package ch.bergturbenthal.infrastructure.event;

import lombok.Value;

@Value
public class RedirectBootAction implements BootAction {
    private String redirectTarget;
}
