package ch.bergturbenthal.infrastructure.service;

import java.util.Optional;

import org.springframework.http.ResponseEntity;

import ch.bergturbenthal.infrastructure.event.BootAction;

public interface ActionProcessor {
    Class<? extends BootAction> actionType();

    Iterable<BootAction> listAvailableActions();

    Optional<ResponseEntity<String>> processAction(BootAction action);
}
