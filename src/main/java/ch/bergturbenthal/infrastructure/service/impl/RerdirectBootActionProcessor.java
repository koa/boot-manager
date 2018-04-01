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
import ch.bergturbenthal.infrastructure.model.BootContext;
import ch.bergturbenthal.infrastructure.model.BootContext.ContextData;
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
    public Optional<ResponseEntity<String>> processAction(final BootContext action) {
        if (action.getAction() instanceof RedirectBootAction) {
            final String redirectTarget = ((RedirectBootAction) action.getAction()).getRedirectTarget() + "/";
            final ServletUriComponentsBuilder contextPath = ServletUriComponentsBuilder.fromCurrentContextPath();
            final Optional<ContextData> context = action.getContext();
            final URI targetUri = context.map(c -> contextPath.path(redirectTarget).path(c.getMachineName()).build().toUri())
                    .orElse(contextPath.path(redirectTarget).build().toUri());
            return Optional.of(ResponseEntity.status(HttpStatus.FOUND).location(targetUri).build());
        } else {
            return Optional.empty();
        }
    }

}
