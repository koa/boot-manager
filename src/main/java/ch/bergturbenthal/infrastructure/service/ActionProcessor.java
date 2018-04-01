package ch.bergturbenthal.infrastructure.service;

import java.util.Optional;

import org.springframework.http.ResponseEntity;

import ch.bergturbenthal.infrastructure.event.BootAction;
import ch.bergturbenthal.infrastructure.model.BootContext;

public interface ActionProcessor {
    Class<? extends BootAction> actionType();

    Iterable<BootAction> listAvailableActions();

    Optional<ResponseEntity<String>> processAction(BootContext action);
}
