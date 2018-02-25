package ch.bergturbenthal.infrastructure.ui.view;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.vaadin.contextmenu.GridContextMenu;
import com.vaadin.contextmenu.GridContextMenu.GridContextMenuOpenListener.GridContextMenuOpenEvent;
import com.vaadin.contextmenu.MenuItem;
import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.provider.GridSortOrderBuilder;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.icons.VaadinIcons;
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
import com.vaadin.ui.components.grid.Editor;

import ch.bergturbenthal.infrastructure.event.AssignMacAddressToMachineEvent;
import ch.bergturbenthal.infrastructure.event.BootAction;
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
import ch.bergturbenthal.infrastructure.service.ActionProcessor;
import ch.bergturbenthal.infrastructure.service.BootLogService.BootLogEntry;
import ch.bergturbenthal.infrastructure.service.LogService;
import ch.bergturbenthal.infrastructure.service.MachineService;
import ch.bergturbenthal.infrastructure.service.MachineService.BootConfigurationEntry;
import ch.bergturbenthal.infrastructure.service.MachineService.ServerData;
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
        private BootAction   bootAction;
    }

    public ServerOverview(final MachineService machineService, final StateService stateService, final LogService logService,
            final Collection<ActionProcessor> actionProcessors) {

        final Supplier<Iterable<BootAction>> availablePatternsSupplier = () -> actionProcessors.stream()
                .flatMap(p -> StreamSupport.stream(p.listAvailableActions().spliterator(), false)).collect(Collectors.toList());

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
        final Function<ServerData, Optional<BootLogEntry>> extractLastLogEntry = t -> {
            final List<BootLogEntry> history = t.getBootHistory();
            if (!history.isEmpty()) {
                return Optional.of(history.get(history.size() - 1));
            }
            return Optional.empty();
        };
        final Column<ServerData, String> lastBootTimeColumn = serverGrid.addColumn(source -> extractLastLogEntry.apply(source)
                .map(e -> e.getTimestamp()).map(t -> t.atOffset(offset.get()).format(formatter.get())).orElse("<no data>"));
        lastBootTimeColumn.setCaption("Last boot time");
        lastBootTimeColumn.setComparator((o1, o2) -> Comparator.<Instant> nullsFirst(Comparator.naturalOrder()).compare(
                extractLastLogEntry.apply(o1).map(e -> e.getTimestamp()).orElse(null),
                extractLastLogEntry.apply(o2).map(e -> e.getTimestamp()).orElse(null)));

        final Column<ServerData, String> lastConfigColumn = serverGrid.addColumn(source -> extractLastLogEntry.apply(source)
                .map(e -> e.getConfiguration().map(v -> v.toString()).orElse("<no configuration>")).orElse("<no data>"));
        lastConfigColumn.setCaption("last configuration");

        final Column<ServerData, Optional<BootAction>> nextConfigurationColumn = serverGrid.addColumn(
                s -> machineService.evaluateNextBootConfiguration(s.getName()), a -> a.map(v -> v.toString()).orElse("<no configuration>"));
        nextConfigurationColumn.setCaption("next boot configuration");
        nextConfigurationColumn.setSortable(false);
        final GridContextMenu<ServerData> contextMenu = new GridContextMenu<>(serverGrid);
        contextMenu.addGridBodyContextMenuListener((final GridContextMenuOpenEvent<ServerData> event) -> {
            contextMenu.removeItems();
            final ServerData item = (ServerData) event.getItem();
            if (item != null) {
                serverGrid.select(item);
                final String serverName = item.getName();

                contextMenu.addItem("show log", showEvent -> {
                    final Window window = new Window("log of " + serverName);
                    final List<BootLogEntry> logCollection = new ArrayList<BootLogEntry>();
                    final ListDataProvider<BootLogEntry> logDataProvider = DataProvider.ofCollection(logCollection);
                    final Grid<BootLogEntry> logGrid = new Grid<>(logDataProvider);

                    final Column<BootLogEntry, String> timestampColumn = logGrid
                            .addColumn(e -> e.getTimestamp().atOffset(offset.get()).format(formatter.get()));
                    timestampColumn.setCaption("Time");
                    timestampColumn.setComparator((o1, o2) -> Comparator.comparing(BootLogEntry::getTimestamp).compare(o1, o2));

                    final Column<BootLogEntry, String> macColumn = logGrid.addColumn(e -> e.getMacAddress().toString());
                    macColumn.setCaption("Mac");

                    final Column<BootLogEntry, String> idColumn = logGrid.addColumn(e -> e.getUuid().map(id -> id.toString()).orElse("<no id>"));
                    idColumn.setCaption("uuid");

                    final Column<BootLogEntry, Optional<BootAction>> configColumn = logGrid.addColumn(e -> e.getConfiguration(),
                            v -> v.map(e -> e.toString()).orElse("<no config>"));
                    configColumn.setCaption("configuration");

                    final Runnable refreshRunnable = () -> {
                        final List<ServerData> listServers = machineService.listServers();
                        for (final ServerData data : listServers) {
                            if (serverName.equals(data.getName())) {
                                logCollection.clear();
                                logCollection.addAll(data.getBootHistory());
                                logDataProvider.refreshAll();
                            }
                        }
                    };
                    refreshRunnable.run();
                    final Disposable registration = stateService.registerForUpdates(() -> getUI().access(refreshRunnable));
                    logGrid.setSizeFull();
                    window.setContent(logGrid);
                    window.addDetachListener(evdetachEvent -> registration.dispose());
                    window.setWidth(60, Unit.EM);
                    window.setHeight(60, Unit.PERCENTAGE);
                    window.center();
                    getUI().addWindow(window);
                });

                final MenuItem nextBootMenu = contextMenu.addItem("set next boot", null);
                final MenuItem defaultBootMenu = contextMenu.addItem("set default boot", null);
                Optional<UUID> exisingOnebootScope = Optional.empty();
                Optional<BootAction> existingOnebootConfiguration = Optional.empty();
                Optional<UUID> exisingdefaultScope = Optional.empty();
                Optional<BootAction> existingDefaultConfiguration = Optional.empty();
                for (final BootConfigurationEntry entry : item.getBootConfiguration()) {
                    if (entry.getScope() instanceof OnebootPatternScope) {
                        exisingOnebootScope = Optional.of(entry.getUuid());
                        existingOnebootConfiguration = Optional.of(entry.getConfiguration());
                    }
                    if (entry.getScope() instanceof DefaultPatternScope) {
                        exisingdefaultScope = Optional.of(entry.getUuid());
                        existingDefaultConfiguration = Optional.of(entry.getConfiguration());
                    }
                }
                final Optional<UUID> foundExistingOnebootScope = exisingOnebootScope;
                final Optional<UUID> foundExistingDefaultScope = exisingdefaultScope;
                for (final BootAction pattern : availablePatternsSupplier.get()) {
                    if (existingOnebootConfiguration.map(e -> e.equals(pattern)).orElse(Boolean.FALSE)) {
                        nextBootMenu.addItem(pattern.toString(), VaadinIcons.CHECK, null);
                    } else {
                        nextBootMenu.addItem(pattern.toString(), selectEvent -> {
                            final UUID id = foundExistingOnebootScope.orElse(UUID.randomUUID());
                            logService.appendEvent(new UpdateMachinePatternEvent(id, item.getName(), pattern, new OnebootPatternScope()));
                        });
                    }
                    if (existingDefaultConfiguration.map(e -> e.equals(pattern)).orElse(Boolean.FALSE)) {
                        defaultBootMenu.addItem(pattern.toString(), VaadinIcons.CHECK, null);
                    } else {
                        defaultBootMenu.addItem(pattern.toString(), selectEvent -> {
                            final UUID id = foundExistingDefaultScope.orElse(UUID.randomUUID());
                            logService.appendEvent(new UpdateMachinePatternEvent(id, item.getName(), pattern, new DefaultPatternScope()));
                        });

                    }
                }
            }
        });
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
                            e -> EditBootConfigurationEntry.builder().scope(e.getScope()).bootAction(e.getConfiguration()).build()));
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
            final Column<UUID, BootAction> configurationColumn = bootConfigurationGrid
                    .addColumn(id -> currentBootConfiguration.computeIfAbsent(id, defaultCreator).getBootAction());
            final List<BootAction> availablePatterns = new ArrayList<>();
            for (final BootAction action : availablePatternsSupplier.get()) {
                availablePatterns.add(action);
            }
            final ComboBox<BootAction> patternEditor = new ComboBox<>("pattern", availablePatterns);
            patternEditor.setEmptySelectionAllowed(false);
            configurationColumn.setEditorComponent(patternEditor, (bean, fieldvalue) -> currentBootConfiguration.compute(bean, (k, v) -> Optional
                    .ofNullable(v).map(e -> e.toBuilder()).orElseGet(() -> EditBootConfigurationEntry.builder()).bootAction(fieldvalue).build()));
            configurationColumn.setEditable(true);
            configurationColumn.setCaption("Pattern");

            final Editor<UUID> editor = bootConfigurationGrid.getEditor();
            editor.setEnabled(true);
            editor.setBuffered(false);
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
                        final BootAction patternName = value.getBootAction();
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
        serverGrid.setSizeFull();

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
