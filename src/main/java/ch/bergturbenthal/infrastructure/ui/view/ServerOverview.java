package ch.bergturbenthal.infrastructure.ui.view;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.provider.GridSortOrderBuilder;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.navigator.View;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.Column;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

import ch.bergturbenthal.infrastructure.event.AssignMacAddressToMachineEvent;
import ch.bergturbenthal.infrastructure.event.DefaultPatternScope;
import ch.bergturbenthal.infrastructure.event.OnebootPatternScope;
import ch.bergturbenthal.infrastructure.event.PatternScope;
import ch.bergturbenthal.infrastructure.event.RemoveMacAddressFromMachineEvent;
import ch.bergturbenthal.infrastructure.event.RemoveMachineEvent;
import ch.bergturbenthal.infrastructure.event.RemoveMachinePatternEvent;
import ch.bergturbenthal.infrastructure.event.RemoveMachineUUIDEvent;
import ch.bergturbenthal.infrastructure.event.RenameMachineEvent;
import ch.bergturbenthal.infrastructure.event.SetMachineUuidEvent;
import ch.bergturbenthal.infrastructure.event.UpdateMachinePatternEvent;
import ch.bergturbenthal.infrastructure.model.MacAddress;
import ch.bergturbenthal.infrastructure.service.LogService;
import ch.bergturbenthal.infrastructure.service.MachineService;
import ch.bergturbenthal.infrastructure.service.MachineService.ServerData;
import ch.bergturbenthal.infrastructure.service.PatternService;
import ch.bergturbenthal.infrastructure.service.StateService;
import ch.bergturbenthal.infrastructure.ui.component.FreeMacSelector;
import ch.bergturbenthal.infrastructure.ui.component.FreeUUIDSelector;
import lombok.Builder;
import lombok.Value;
import reactor.core.Disposable;

@SpringView(name = "")
public class ServerOverview extends CustomComponent implements View {

    @Value
    @Builder(toBuilder = true)
    private static class EditBootConfigurationEntry {
        private PatternScope scope;
        private String       patternName;
    }

    public ServerOverview(final MachineService machineService, final StateService stateService, final LogService logService,
            final PatternService patternService) {
        final List<ServerData> servers = new ArrayList<>();
        final ListDataProvider<ServerData> dataProvider = DataProvider.ofCollection(servers);
        final Grid<ServerData> serverGrid = new Grid<>("Servers", dataProvider);

        serverGrid.addColumn(ServerData::getName).setCaption("Name");
        final Column<ServerData, String> macAddressColumn = serverGrid
                .addColumn(d -> d.getMacs().stream().map(a -> a.toString()).collect(Collectors.joining(", ")));
        macAddressColumn.setCaption("Mac Addresses");
        macAddressColumn.setSortable(false);

        final AtomicReference<ZoneOffset> offset = new AtomicReference<ZoneOffset>(ZoneOffset.UTC);
        final AtomicReference<DateTimeFormatter> formatter = new AtomicReference<DateTimeFormatter>(
                DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM));
        final Column<ServerData, String> lastBootTimeColumn = serverGrid
                .addColumn(source -> source.getLastBootTime().map(t -> t.atOffset(offset.get()).format(formatter.get())).orElse("<no data>"));
        lastBootTimeColumn.setCaption("Last boot time");
        lastBootTimeColumn.setComparator((o1, o2) -> Comparator.<Instant> nullsFirst(Comparator.naturalOrder())
                .compare(o1.getLastBootTime().orElse(null), o2.getLastBootTime().orElse(null)));

        final Column<ServerData, String> nextConfigurationColumn = serverGrid
                .addColumn(s -> machineService.evaluateNextBootConfiguration(s.getName()).orElse("<default>"));
        nextConfigurationColumn.setCaption("next boot configuration");
        nextConfigurationColumn.setSortable(false);

        serverGrid.setSortOrder(new GridSortOrderBuilder<ServerData>().thenDesc(lastBootTimeColumn).build());
        final Button addServerButton = new Button("add Server");

        addServerButton.addClickListener(event -> {
            final Window window = new Window("create server");
            final FormLayout formLayout = new FormLayout();
            final TextField nameField = new TextField("name");
            final FreeUUIDSelector uuidSelector = createUUIDSelector(machineService, stateService, Optional.empty());
            final FreeMacSelector macSelector = createMacSelector(machineService, stateService, Collections.emptySet());
            formLayout.addComponent(nameField);
            formLayout.addComponent(uuidSelector);
            formLayout.addComponent(macSelector);
            formLayout.addComponent(new Button("Add", ev -> {
                final String name = nameField.getValue();
                final UUID selectedUUID = uuidSelector.getValue();
                if (selectedUUID != null) {
                    logService.appendEvent(new SetMachineUuidEvent(name, selectedUUID));
                }
                final Set<MacAddress> macs = macSelector.getValue();
                for (final MacAddress macAddress : macs) {
                    logService.appendEvent(new AssignMacAddressToMachineEvent(name, macAddress));
                }
                window.close();
            }));
            window.setContent(formLayout);
            window.setWidth(40, Unit.EM);
            window.center();
            getUI().addWindow(window);
        });

        final Button removeServerButton = new Button("remove Server");
        removeServerButton.addClickListener(event -> {
            for (final String name : serverGrid.getSelectedItems().stream().map(e -> e.getName()).collect(Collectors.toList())) {
                logService.appendEvent(new RemoveMachineEvent(name));
            }
        });
        removeServerButton.setEnabled(false);

        final Button editServerButton = new Button("edit Server");
        editServerButton.setEnabled(false);
        editServerButton.addClickListener(event -> {
            final Set<ServerData> selectedItems = serverGrid.getSelectedItems();
            if (selectedItems.size() != 1) {
                return;
            }
            final ServerData serverDataBeforeEdit = selectedItems.iterator().next();
            final UUID uuidBefore = serverDataBeforeEdit.getUuid();

            final Window window = new Window("edit server");
            final FormLayout formLayout = new FormLayout();
            final TextField nameField = new TextField("name");
            nameField.setValue(serverDataBeforeEdit.getName());
            formLayout.addComponent(nameField);

            final FreeUUIDSelector uuidSelector = createUUIDSelector(machineService, stateService, Optional.ofNullable(uuidBefore));
            formLayout.addComponent(uuidSelector);

            final Set<MacAddress> macsBefore = new HashSet<>(serverDataBeforeEdit.getMacs());
            if (uuidBefore != null) {
                macsBefore.removeAll(machineService.macsOfUUID(uuidBefore));
            }

            final FreeMacSelector macSelector = createMacSelector(machineService, stateService, macsBefore);
            formLayout.addComponent(macSelector);

            final Map<UUID, EditBootConfigurationEntry> bootConfigurationBeforeEdit = serverDataBeforeEdit.getBootConfiguration().stream()
                    .collect(Collectors.toMap(e -> e.getUuid(),
                            e -> EditBootConfigurationEntry.builder().scope(e.getScope()).patternName(e.getConfiguration()).build()));
            final HashMap<UUID, EditBootConfigurationEntry> currentBootConfiguration = new HashMap<>(bootConfigurationBeforeEdit);
            final Set<UUID> konfigurationsList = new TreeSet<UUID>(currentBootConfiguration.keySet());
            final ListDataProvider<UUID> configurationsProvider = DataProvider.ofCollection(konfigurationsList);
            final Grid<UUID> bootConfigurationGrid = new Grid<>("boot configuration", configurationsProvider);

            final Function<? super UUID, ? extends EditBootConfigurationEntry> defaultCreator = k -> EditBootConfigurationEntry.builder().build();
            final Column<UUID, PatternScope> scopeColumn = bootConfigurationGrid
                    .addColumn(id -> currentBootConfiguration.computeIfAbsent(id, defaultCreator).getScope());
            final ComboBox<PatternScope> scopeEditor = new ComboBox<>("scope", Arrays.asList(new DefaultPatternScope(), new OnebootPatternScope()));
            scopeEditor.setEmptySelectionAllowed(false);
            scopeColumn.setEditorComponent(scopeEditor, (bean, fieldvalue) -> currentBootConfiguration.compute(bean, (k, v) -> Optional.ofNullable(v)
                    .map(e -> e.toBuilder()).orElseGet(() -> EditBootConfigurationEntry.builder()).scope(fieldvalue).build()));
            scopeColumn.setEditable(true);
            scopeColumn.setCaption("Scope");
            final Column<UUID, String> configurationColumn = bootConfigurationGrid
                    .addColumn(id -> currentBootConfiguration.computeIfAbsent(id, defaultCreator).getPatternName());
            final ComboBox<String> patternEditor = new ComboBox<>("pattern", new TreeSet<>(patternService.listPatterns().keySet()));
            patternEditor.setEmptySelectionAllowed(false);
            configurationColumn.setEditorComponent(patternEditor, (bean, fieldvalue) -> currentBootConfiguration.compute(bean, (k, v) -> Optional
                    .ofNullable(v).map(e -> e.toBuilder()).orElseGet(() -> EditBootConfigurationEntry.builder()).patternName(fieldvalue).build()));
            configurationColumn.setEditable(true);
            configurationColumn.setCaption("Pattern");

            bootConfigurationGrid.getEditor().setEnabled(true);
            formLayout.addComponent(bootConfigurationGrid);
            final Button saveButton = new Button("save", ev -> {
                final String nameBefore = serverDataBeforeEdit.getName();
                for (final MacAddress newMac : macSelector.getValue()) {
                    if (!macsBefore.remove(newMac)) {
                        logService.appendEvent(new AssignMacAddressToMachineEvent(nameBefore, newMac));
                    }
                }
                for (final MacAddress macAddress : macsBefore) {
                    logService.appendEvent(new RemoveMacAddressFromMachineEvent(nameBefore, macAddress));
                }
                final UUID uuidAfter = uuidSelector.getValue();
                if (uuidAfter == null) {
                    if (uuidBefore != null) {
                        logService.appendEvent(new RemoveMachineUUIDEvent(nameBefore));
                    }
                } else {
                    if (!uuidAfter.equals(uuidBefore)) {
                        logService.appendEvent(new SetMachineUuidEvent(nameBefore, uuidAfter));
                    }

                }
                for (final Entry<UUID, EditBootConfigurationEntry> currentEntry : currentBootConfiguration.entrySet()) {
                    final UUID id = currentEntry.getKey();
                    final EditBootConfigurationEntry entryBefore = bootConfigurationBeforeEdit.remove(id);
                    final EditBootConfigurationEntry value = currentEntry.getValue();
                    if (!Objects.equals(entryBefore, value)) {
                        final String patternName = value.getPatternName();
                        final PatternScope scope = value.getScope();
                        if (scope != null && patternName != null) {
                            logService.appendEvent(new UpdateMachinePatternEvent(id, nameBefore, patternName, scope));
                        }
                    }
                }
                for (final UUID id : bootConfigurationBeforeEdit.keySet()) {
                    logService.appendEvent(new RemoveMachinePatternEvent(id));
                }
                final String name = nameField.getValue();
                if (!nameBefore.equals(name)) {
                    logService.appendEvent(new RenameMachineEvent(nameBefore, name));
                }
                window.close();
            });
            final Button addConfigurationButton = new Button("add boot configuration", ev -> {
                konfigurationsList.add(UUID.randomUUID());
                configurationsProvider.refreshAll();
            });

            formLayout.addComponent(new HorizontalLayout(saveButton, addConfigurationButton));
            window.setContent(formLayout);
            window.setWidth(40, Unit.EM);
            window.center();
            getUI().addWindow(window);
        });

        serverGrid.addSelectionListener(event -> {
            final Set<ServerData> allSelectedItems = event.getAllSelectedItems();
            removeServerButton.setEnabled(!allSelectedItems.isEmpty());
            editServerButton.setEnabled(allSelectedItems.size() == 1);
        });
        serverGrid.setWidth(100, Unit.PERCENTAGE);
        serverGrid.setHeight(100, Unit.PERCENTAGE);

        final VerticalLayout mainPanel = new VerticalLayout(serverGrid, new HorizontalLayout(addServerButton, removeServerButton, editServerButton));
        mainPanel.setExpandRatio(serverGrid, 1);
        mainPanel.setHeight(100, Unit.PERCENTAGE);
        setCompositionRoot(mainPanel);
        setHeight(100, Unit.PERCENTAGE);
        final Runnable refreshRunnable = () -> {
            final List<ServerData> currentServers = machineService.listServers();
            servers.clear();
            servers.addAll(currentServers);
            dataProvider.refreshAll();
        };
        final Disposable registration = stateService.registerForUpdates(() -> getUI().access(refreshRunnable));
        addAttachListener(event -> {
            final int timezoneOffset = getUI().getPage().getWebBrowser().getTimezoneOffset();
            offset.set(ZoneOffset.ofTotalSeconds(timezoneOffset / 1000));
            formatter.set(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(getLocale()));
            refreshRunnable.run();
        });
        addDetachListener(event -> registration.dispose());
    }

    private FreeMacSelector createMacSelector(final MachineService machineService, final StateService stateService,
            final Set<MacAddress> macsBefore) {
        final FreeMacSelector macSelector = new FreeMacSelector("select mac", machineService, stateService);
        macSelector.setValue(macsBefore);
        macSelector.setWidth(100, Unit.PERCENTAGE);
        return macSelector;
    }

    private FreeUUIDSelector createUUIDSelector(final MachineService machineService, final StateService stateService,
            final Optional<UUID> uuidBefore) {
        final FreeUUIDSelector uuidSelector = new FreeUUIDSelector(stateService, machineService);
        uuidSelector.setCaption("UUID");
        uuidBefore.ifPresent(id -> uuidSelector.setValue(id));
        uuidSelector.setWidth(100, Unit.PERCENTAGE);
        return uuidSelector;
    }
}
