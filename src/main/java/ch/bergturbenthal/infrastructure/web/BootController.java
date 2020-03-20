package ch.bergturbenthal.infrastructure.web;

import ch.bergturbenthal.infrastructure.event.PatternBootAction;
import ch.bergturbenthal.infrastructure.event.RedirectBootAction;
import ch.bergturbenthal.infrastructure.model.BootContext;
import ch.bergturbenthal.infrastructure.model.MacAddress;
import ch.bergturbenthal.infrastructure.service.ActionProcessor;
import ch.bergturbenthal.infrastructure.service.MachineService;
import ch.bergturbenthal.infrastructure.service.PatternService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController()
public class BootController {

  @Autowired
  private MachineService machineService;

  @Autowired
  private PatternService patternService;

  @Autowired
  private Collection<ActionProcessor> knownActionProcessors;

  private ResponseEntity<String> processBootAction(final BootContext action) {
    for (final ActionProcessor actionProcessor : knownActionProcessors) {
      if (!actionProcessor.actionType().isInstance(action.getAction())) {
        continue;
      }
      final Optional<ResponseEntity<String>> response = actionProcessor.processAction(action);
      if (response.isPresent()) {
        return response.get();
      }
    }
    if (action.getAction() instanceof PatternBootAction) {
      final PatternBootAction patternAction = (PatternBootAction) action.getAction();
      final String patternData = patternService.listPatterns().get(patternAction.getPatternName());
      if (patternData == null) {
        return ResponseEntity.notFound().build();
      } else {
        return ResponseEntity.ok(patternData);
      }
    }
    if (action.getAction() instanceof RedirectBootAction) {
      final String redirectTarget = ((RedirectBootAction) action.getAction()).getRedirectTarget();
      final ServletUriComponentsBuilder contextPath = ServletUriComponentsBuilder.fromCurrentContextPath();
      final URI targetUri = contextPath.path(redirectTarget).build().toUri();
      return ResponseEntity.status(HttpStatus.FOUND).location(targetUri).build();
    }
    return ResponseEntity.notFound().build();
  }

  @GetMapping("/ipxe")
  public ResponseEntity<String> processPxeBoot(@RequestParam("mac") final String macAddress,
                                               @RequestParam("uuid") final Optional<String> hostUuid) {
    final MacAddress parsedMac = MacAddress.parseAddress(macAddress);

    final Optional<UUID> cleanUuid = hostUuid.map(UUID::fromString)
                                             .flatMap(id -> id.getLeastSignificantBits() == 0 && id.getMostSignificantBits() == 0 ? Optional
                                                     .empty() : Optional.of(id));

    return machineService.processBoot(cleanUuid, parsedMac)
                         .map(this::processBootAction)
                         .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @GetMapping("/pattern/{filename:.*}")
  public ResponseEntity<String> readPattern(@PathVariable("filename") final String filename) {
    return Optional.ofNullable(patternService.listPatterns().get(filename)).map(ResponseEntity::ok)
                   .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @GetMapping(value = "/ipxe", headers = {"user-agent=debian-installer"})
  public ResponseEntity<String> getDebianInstallFile() {
    return ResponseEntity.ok("");
  }
}
