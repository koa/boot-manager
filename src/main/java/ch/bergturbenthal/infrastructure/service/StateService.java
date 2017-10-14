package ch.bergturbenthal.infrastructure.service;

import reactor.core.Disposable;

public interface StateService {
    Disposable registerForUpdates(Runnable run);
}
