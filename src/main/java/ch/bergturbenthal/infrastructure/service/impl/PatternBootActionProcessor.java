package ch.bergturbenthal.infrastructure.service.impl;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import ch.bergturbenthal.infrastructure.event.BootAction;
import ch.bergturbenthal.infrastructure.event.PatternBootAction;
import ch.bergturbenthal.infrastructure.model.BootContext;
import ch.bergturbenthal.infrastructure.service.ActionProcessor;
import ch.bergturbenthal.infrastructure.service.PatternService;

@Service
public class PatternBootActionProcessor implements ActionProcessor {
    @Autowired
    private PatternService patternService;

    @Override
    public Class<PatternBootAction> actionType() {
        return PatternBootAction.class;
    }

    @Override
    public Iterable<BootAction> listAvailableActions() {
        return () -> patternService.listPatterns().keySet().stream().map(v -> (BootAction) new PatternBootAction(v)).iterator();
    }

    @Override
    public Optional<ResponseEntity<String>> processAction(final BootContext action) {
        if (action.getAction() instanceof PatternBootAction) {
            final String patternData = patternService.listPatterns().get(((PatternBootAction) action.getAction()).getPatternName());
            if (patternData == null) {
                return Optional.empty();
            } else {
                return Optional.of(ResponseEntity.ok(patternData));
            }
        } else {
            return Optional.empty();
        }

    }

}
