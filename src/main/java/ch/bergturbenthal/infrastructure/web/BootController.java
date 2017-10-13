package ch.bergturbenthal.infrastructure.web;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ch.bergturbenthal.infrastructure.model.MacAddress;
import ch.bergturbenthal.infrastructure.service.MachineService;

@RestController()
public class BootController {

    @Autowired
    private MachineService machineService;

    @GetMapping("/ipxe")
    public String processPxeBoot(@RequestParam("mac") final String macAddress, @RequestParam("uuid") final Optional<UUID> hostUuid) {
        final MacAddress parsedMac = MacAddress.parseAddress(macAddress);
        final Optional<UUID> cleanUuid = hostUuid
                .flatMap(id -> id.getLeastSignificantBits() == 0 && id.getMostSignificantBits() == 0 ? Optional.empty() : Optional.of(id));

        return machineService.processBoot(cleanUuid, parsedMac).orElse("sanboot --no-describe --drive 0x80");
    }
}
