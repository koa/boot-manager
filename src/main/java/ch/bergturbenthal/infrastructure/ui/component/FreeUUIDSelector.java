package ch.bergturbenthal.infrastructure.ui.component;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.ListSelect;

import ch.bergturbenthal.infrastructure.model.MacAddress;
import ch.bergturbenthal.infrastructure.service.MachineService;
import ch.bergturbenthal.infrastructure.service.StateService;
import reactor.core.Disposable;

public class FreeUUIDSelector extends CustomField<UUID> {
    private final Supplier<Optional<UUID>> valueReader;
    private final Consumer<Optional<UUID>> valueWriter;
    private final Component                component;

    public FreeUUIDSelector(final StateService stateService, final MachineService machineService) {
        final ListSelect<UUID> uuidSelect = new ListSelect<>();
        final Map<UUID, Collection<MacAddress>> foundUUIDs = new TreeMap<>();
        final ListDataProvider<UUID> dataProvider = DataProvider.ofCollection(foundUUIDs.keySet());
        uuidSelect.setItemCaptionGenerator(
                id -> id.toString() + ": " + foundUUIDs.get(id).stream().map(a -> a.toString()).collect(Collectors.joining(", ")));
        uuidSelect.setDataProvider(dataProvider);
        final AtomicReference<Optional<UUID>> additionalUUID = new AtomicReference<Optional<UUID>>(Optional.empty());
        final Runnable refresh = () -> {
            final Set<UUID> selectedValue = uuidSelect.getValue();
            foundUUIDs.clear();
            foundUUIDs.putAll(machineService.listFreeUUIDs());
            additionalUUID.get().ifPresent(v -> foundUUIDs.put(v, Collections.emptyList()));
            dataProvider.refreshAll();
            uuidSelect.setValue(selectedValue);
        };
        final Disposable registration = stateService.registerForUpdates(() -> getUI().access(refresh));
        addDetachListener(event -> {
            registration.dispose();
        });
        valueWriter = value -> {
            additionalUUID.set(value);
            uuidSelect.setValue(value.map(v -> Collections.singleton(v)).orElse(Collections.emptySet()));
            refresh.run();
        };
        valueReader = () -> uuidSelect.getOptionalValue().map(v -> v.iterator().next());
        addAttachListener(event -> {
            refresh.run();
        });
        uuidSelect.setSizeFull();
        component = uuidSelect;
    }

    @Override
    protected void doSetValue(final UUID value) {
        valueWriter.accept(Optional.ofNullable(value));
    }

    @Override
    public UUID getValue() {
        return valueReader.get().orElse(null);
    }

    @Override
    protected Component initContent() {
        return component;
    }

}
