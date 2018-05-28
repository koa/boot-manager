package ch.bergturbenthal.infrastructure.web;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import ch.bergturbenthal.infrastructure.service.MachineService;
import ch.bergturbenthal.infrastructure.service.RedirectTargetManager;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("coreos")
public class CoreosController {

    private final MachineService machineService;

    public CoreosController(final RedirectTargetManager manager, final MachineService machineService) {
        this.machineService = machineService;
        manager.addRedirectTarget("coreos/install");
    }

    private Map<String, Object> createVariables(final String hostname) {
        final Map<String, Object> variables = new HashMap<>();
        variables.put("hostname", hostname);
        machineService.findServerByName(hostname).ifPresent(serverData -> {
            variables.putAll(serverData.getProperties());
            serverData.getMacs().stream().sorted().map(m -> m.toString()).findFirst().ifPresent(mac -> variables.put("mac", mac));
        });
        final String uriString = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        variables.put("contextRoot", uriString);
        final String baseUrl = Optional.ofNullable(System.getenv("BASE_URL")).orElse("http://stable.release.core-os.net/amd64-usr");
        variables.put("baseUrl", baseUrl);
        final File sshDir = new File(new File(System.getProperty("user.home")), ".ssh");
        final String sshKey = System.getenv("SSH_KEY");
        log.info("SSH-Key: " + sshKey);
        if (sshKey != null) {
            variables.put("sshKey", sshKey);
        } else {
            final File rsaKey = new File(sshDir, "id_rsa.pub");
            if (rsaKey.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(rsaKey))) {
                    final String key = reader.readLine();
                    variables.put("sshKey", key);
                } catch (final IOException ex) {
                    log.error("Cannot read public ssh key", ex);
                }
            }
        }
        return variables;
    }

    @GetMapping("ignition-disk/{hostname}")
    public ModelAndView ignitionDisk(@PathVariable("hostname") final String hostname) {
        return new ModelAndView("ignition-disk", createVariables(hostname));
    }

    @GetMapping("ignition-ram/{hostname}")
    public ModelAndView ignitionRam(@PathVariable("hostname") final String hostname) {
        return new ModelAndView("ignition-ram", createVariables(hostname));
    }

    @GetMapping("install/{hostname}")
    public ModelAndView pxe(@PathVariable("hostname") final String hostname) {
        return new ModelAndView("install-coreos", createVariables(hostname));
    }

    @GetMapping("install-script/{hostname}")
    public ModelAndView script(@PathVariable("hostname") final String hostname) {
        return new ModelAndView("install", createVariables(hostname));
    }
}
