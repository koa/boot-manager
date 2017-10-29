package ch.bergturbenthal.infrastructure.web;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ch.bergturbenthal.infrastructure.model.MacAddress;
import ch.bergturbenthal.infrastructure.service.MachineService;
import ch.bergturbenthal.infrastructure.service.PatternService;

@RestController()
public class BootController {

    @Autowired
    private MachineService machineService;

    @Autowired
    private PatternService patternService;

    @GetMapping("/ipxe")
    public ResponseEntity<String> processPxeBoot(@RequestParam("mac") final String macAddress, @RequestParam("uuid") final Optional<UUID> hostUuid) {
        final MacAddress parsedMac = MacAddress.parseAddress(macAddress);
        final Optional<UUID> cleanUuid = hostUuid
                .flatMap(id -> id.getLeastSignificantBits() == 0 && id.getMostSignificantBits() == 0 ? Optional.empty() : Optional.of(id));

        return machineService.processBoot(cleanUuid, parsedMac).map(body -> ResponseEntity.ok(body))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/pattern/{filename:.*}")
    public ResponseEntity<String> readPattern(@PathVariable("filename") final String filename) {
        return Optional.ofNullable(patternService.listPatterns().get(filename)).map(body -> ResponseEntity.ok(body))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
