package ch.bergturbenthal.infrastructure.ui.view;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.navigator.View;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Grid;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

import ch.bergturbenthal.infrastructure.event.AssignMacAddressToMachineEvent;
import ch.bergturbenthal.infrastructure.event.RemoveMacAddressFromMachineEvent;
import ch.bergturbenthal.infrastructure.event.RemoveMachineEvent;
import ch.bergturbenthal.infrastructure.event.RenameMachineEvent;
import ch.bergturbenthal.infrastructure.model.MacAddress;
import ch.bergturbenthal.infrastructure.service.LogService;
import ch.bergturbenthal.infrastructure.service.MachineService;
import ch.bergturbenthal.infrastructure.service.MachineService.ServerData;
import ch.bergturbenthal.infrastructure.ui.component.FreeMacSelector;

@SpringView(name = "")
public class ServerOverview extends CustomComponent implements View {
    private static interface ServerEntry {

    }

    public ServerOverview(final MachineService machineService, final LogService logService) {
        final List<ServerData> servers = new ArrayList<>();
        final ListDataProvider<ServerData> dataProvider = DataProvider.ofCollection(servers);
        final Grid<ServerData> serverGrid = new Grid<>("Servers", dataProvider);

        serverGrid.addColumn(ServerData::getName).setCaption("Name");
        serverGrid.addColumn(d -> d.getMacs().stream().map(a -> a.toString()).collect(Collectors.joining(", "))).setCaption("Mac Addresses");

        final AtomicReference<ZoneOffset> offset = new AtomicReference<ZoneOffset>(ZoneOffset.UTC);
        final AtomicReference<DateTimeFormatter> formatter = new AtomicReference<DateTimeFormatter>(
                DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM));
        serverGrid.addColumn(source -> source.getLastBootTime().atOffset(offset.get()).format(formatter.get())).setCaption("Last boot time");

        final Button addServerButton = new Button("add Server");

        addServerButton.addClickListener(event -> {
            final Window window = new Window("create server");
            final FormLayout formLayout = new FormLayout();
            final TextField nameField = new TextField("name");
            final FreeMacSelector macSelector = new FreeMacSelector("select mac", machineService);
            formLayout.addComponent(nameField);
            formLayout.addComponent(macSelector);
            formLayout.addComponent(new Button("Add", ev -> {
                final String name = nameField.getValue();
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
            final Window window = new Window("edit server");
            final FormLayout formLayout = new FormLayout();
            final TextField nameField = new TextField("name");
            nameField.setValue(serverDataBeforeEdit.getName());
            final FreeMacSelector macSelector = new FreeMacSelector("select mac", machineService);
            final Set<MacAddress> macsBefore = new HashSet<>(serverDataBeforeEdit.getMacs());
            macSelector.setValue(macsBefore);
            formLayout.addComponent(nameField);
            formLayout.addComponent(macSelector);
            formLayout.addComponent(new Button("save", ev -> {
                final String nameBefore = serverDataBeforeEdit.getName();
                for (final MacAddress newMac : macSelector.getValue()) {
                    if (!macsBefore.remove(newMac)) {
                        logService.appendEvent(new AssignMacAddressToMachineEvent(nameBefore, newMac));
                    }
                }
                for (final MacAddress macAddress : macsBefore) {
                    logService.appendEvent(new RemoveMacAddressFromMachineEvent(nameBefore, macAddress));
                }
                final String name = nameField.getValue();
                if (!nameBefore.equals(name)) {
                    logService.appendEvent(new RenameMachineEvent(nameBefore, name));
                }
                window.close();
            }));
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
        final VerticalLayout mainPanel = new VerticalLayout(serverGrid, new HorizontalLayout(addServerButton, removeServerButton, editServerButton));
        setCompositionRoot(mainPanel);
        final Runnable refreshRunnable = () -> {
            final List<ServerData> currentServers = machineService.listServers();
            servers.clear();
            servers.addAll(currentServers);
            dataProvider.refreshAll();
        };
        machineService.registerForUpdates(() -> getUI().access(refreshRunnable));
        addAttachListener(event -> {
            final int timezoneOffset = getUI().getPage().getWebBrowser().getTimezoneOffset();
            offset.set(ZoneOffset.ofTotalSeconds(timezoneOffset / 1000));
            formatter.set(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(getLocale()));
            refreshRunnable.run();
        });
    }
}
