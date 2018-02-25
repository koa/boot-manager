package ch.bergturbenthal.infrastructure.service.impl;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import ch.bergturbenthal.infrastructure.event.BootAction;
import ch.bergturbenthal.infrastructure.event.RedirectBootAction;
import ch.bergturbenthal.infrastructure.service.ActionProcessor;
import ch.bergturbenthal.infrastructure.service.RedirectTargetManager;

@Service
public class RerdirectBootActionProcessor implements ActionProcessor, RedirectTargetManager {
    private final AtomicReference<Collection<String>> knownRedirectTargets = new AtomicReference<Collection<String>>(Collections.emptyList());

    @Override
    public Class<RedirectBootAction> actionType() {
        return RedirectBootAction.class;
    }

    @Override
    public void addRedirectTarget(final String target) {
        knownRedirectTargets.updateAndGet(l -> {
            final Set<String> newData = new HashSet<>(l);
            newData.add(target);
            return Collections.unmodifiableCollection(newData);
        });
    }

    @Override
    public Iterable<BootAction> listAvailableActions() {
        return () -> knownRedirectTargets.get().stream().map(t -> (BootAction) new RedirectBootAction(t)).iterator();
    }

    @Override
    public Iterable<String> listRedirectTargets() {
        return () -> knownRedirectTargets.get().iterator();
    }

    @Override
    public Optional<ResponseEntity<String>> processAction(final BootAction action) {
        if (action instanceof RedirectBootAction) {
            final String redirectTarget = ((RedirectBootAction) action).getRedirectTarget();
            final ServletUriComponentsBuilder contextPath = ServletUriComponentsBuilder.fromCurrentContextPath();
            final URI targetUri = contextPath.path(redirectTarget).build().toUri();
            return Optional.of(ResponseEntity.status(HttpStatus.FOUND).location(targetUri).build());
        } else {
            return Optional.empty();
        }
    }

}
