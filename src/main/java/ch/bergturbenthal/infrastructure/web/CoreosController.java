package ch.bergturbenthal.infrastructure.web;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import ch.bergturbenthal.infrastructure.service.RedirectTargetManager;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("coreos")
public class CoreosController {
    public CoreosController(final RedirectTargetManager manager) {
        manager.addRedirectTarget("coreos/install");
    }

    private Map<String, Object> createVariables() {
        final Map<String, Object> variables = new HashMap<>();
        final String uriString = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        variables.put("contextRoot", uriString);
        variables.put("baseUrl", "http://stable.release.core-os.net/amd64-usr/current");
        final File sshDir = new File(new File(System.getProperty("user.home")), ".ssh");
        final File rsaKey = new File(sshDir, "id_rsa.pub");
        if (rsaKey.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(rsaKey))) {
                final String key = reader.readLine();
                variables.put("sshKey", key);
            } catch (final IOException ex) {
                log.error("Cannot read public ssh key", ex);
            }
        }
        return variables;
    }

    @GetMapping("ignition-disk")
    public ModelAndView ignitionDisk() {
        return new ModelAndView("ignition-disk", createVariables());
    }

    @GetMapping("ignition-ram")
    public ModelAndView ignitionRam() {
        return new ModelAndView("ignition-ram", createVariables());
    }

    @GetMapping("install")
    public ModelAndView pxe() {
        return new ModelAndView("install-coreos", createVariables());
    }

    @GetMapping("install-script")
    public ModelAndView script() {
        return new ModelAndView("install", createVariables());
    }
}
