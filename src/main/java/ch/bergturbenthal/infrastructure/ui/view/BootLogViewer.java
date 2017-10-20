package ch.bergturbenthal.infrastructure.ui.view;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicReference;

import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.navigator.View;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.Column;

import ch.bergturbenthal.infrastructure.service.BootLogService;
import ch.bergturbenthal.infrastructure.service.BootLogService.BootLogEntry;
import ch.bergturbenthal.infrastructure.service.StateService;
import reactor.core.Disposable;

@SpringView(name = "boot-log")
public class BootLogViewer extends CustomComponent implements View {
    public BootLogViewer(final StateService stateService, final BootLogService bootLogService) {
        final Collection<BootLogEntry> currentVisibleEntries = new ArrayList<>();
        final ListDataProvider<BootLogEntry> dataProvider = DataProvider.ofCollection(currentVisibleEntries);
        final Grid<BootLogEntry> logGrid = new Grid<>(dataProvider);

        final AtomicReference<ZoneOffset> offset = new AtomicReference<ZoneOffset>(ZoneOffset.UTC);

        final AtomicReference<DateTimeFormatter> formatter = new AtomicReference<DateTimeFormatter>(
                DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM));

        final Column<BootLogEntry, String> timestampColumn = logGrid
                .addColumn(source -> source.getTimestamp().atOffset(offset.get()).format(formatter.get()));
        timestampColumn.setCaption("Timestamp");
        final Comparator<BootLogEntry> comparing = Comparator.comparing(BootLogEntry::getTimestamp);
        timestampColumn.setComparator((o1, o2) -> comparing.compare(o1, o2));

        final Column<BootLogEntry, String> macAddressColumn = logGrid.addColumn(e -> e.getMacAddress().toString());
        macAddressColumn.setCaption("Mac Address");

        final Column<BootLogEntry, String> uuidColumn = logGrid.addColumn(e -> e.getUuid().map(u -> u.toString()).orElse("<no uuid>"));
        uuidColumn.setCaption("UUID");

        final Column<BootLogEntry, String> configColumn = logGrid.addColumn(e -> e.getConfiguration().orElse("<no configuration>"));
        configColumn.setCaption("Configuration");

        logGrid.setSizeFull();
        setCompositionRoot(logGrid);
        setSizeFull();
        final Runnable updater = () -> {
            currentVisibleEntries.clear();
            currentVisibleEntries.addAll(bootLogService.readLastNEntries(50));
            dataProvider.refreshAll();
        };
        final AtomicReference<Disposable> registration = new AtomicReference<Disposable>(null);
        addAttachListener(event -> {

            final int timezoneOffset = getUI().getPage().getWebBrowser().getTimezoneOffset();
            offset.set(ZoneOffset.ofTotalSeconds(timezoneOffset / 1000));
            formatter.set(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(getLocale()));

            final Disposable registerForUpdates = stateService.registerForUpdates(() -> getUI().access(updater));
            final Disposable oldRegistration = registration.getAndSet(registerForUpdates);
            if (oldRegistration != null) {
                oldRegistration.dispose();
            }
            updater.run();
        });
        addDetachListener(event -> {
            final Disposable oldRegistration = registration.getAndSet(null);
            if (oldRegistration != null) {
                oldRegistration.dispose();
            }
        });
    }
}
