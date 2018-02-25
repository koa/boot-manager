package ch.bergturbenthal.infrastructure.service;

public interface RedirectTargetManager {
    void addRedirectTarget(String target);

    Iterable<String> listRedirectTargets();
}
