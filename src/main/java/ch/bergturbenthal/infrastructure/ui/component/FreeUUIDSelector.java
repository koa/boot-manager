package ch.bergturbenthal.infrastructure.ui.component;

import ch.bergturbenthal.infrastructure.model.MacAddress;
import ch.bergturbenthal.infrastructure.service.MachineService;
import ch.bergturbenthal.infrastructure.service.StateService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.server.Command;
import reactor.core.Disposable;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class FreeUUIDSelector {


  public static ListBox<UUID> createSelector(final StateService stateService, final MachineService machineService) {
    final ListBox<UUID> uuidSelect = new ListBox<>();
    final Map<UUID, Collection<MacAddress>> foundUUIDs = new TreeMap<>();
    final ListDataProvider<UUID> dataProvider = DataProvider.ofCollection(foundUUIDs.keySet());
    uuidSelect.setRenderer(new ComponentRenderer<>(id -> new Label(id.toString() + ": " + foundUUIDs.get(id)
                                                                                                    .stream()
                                                                                                    .map(MacAddress::toString)
                                                                                                    .collect(Collectors.joining(
                                                                                                            ", ")))));

    uuidSelect.setDataProvider(dataProvider);
    final AtomicReference<Optional<UUID>> additionalUUID = new AtomicReference<>(Optional.empty());
    final Command refresh = () -> {
      final Optional<UUID> selectedValue = uuidSelect.getOptionalValue();
      foundUUIDs.clear();
      foundUUIDs.putAll(machineService.listFreeUUIDs());
      additionalUUID.get().ifPresent(v -> foundUUIDs.put(v, Collections.emptyList()));
      dataProvider.refreshAll();
      selectedValue.ifPresent(uuidSelect::setValue);
    };

    AtomicReference<Disposable> currrentDisposable = new AtomicReference<>();
    Consumer<Disposable> disposableConsumer = disposable -> {
      final Disposable disposable1 = currrentDisposable.getAndSet(disposable);
      if (disposable1 != null)
        disposable1.dispose();
    };
    uuidSelect.addAttachListener(event -> {
      final UI ui = event.getUI();
      disposableConsumer.accept(stateService.registerForUpdates(() -> ui.access(refresh)));
      refresh.execute();
    });
    uuidSelect.addDetachListener(event -> disposableConsumer.accept(null));
    return uuidSelect;
  }
}
