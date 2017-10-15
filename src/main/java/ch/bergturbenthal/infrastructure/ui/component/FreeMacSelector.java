package ch.bergturbenthal.infrastructure.ui.component;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.ListSelect;

import ch.bergturbenthal.infrastructure.model.MacAddress;
import ch.bergturbenthal.infrastructure.service.MachineService;
import ch.bergturbenthal.infrastructure.service.StateService;
import reactor.core.Disposable;

public class FreeMacSelector extends CustomComponent {

    private final Supplier<Set<MacAddress>> macAddressReader;
    private final Consumer<Set<MacAddress>> macAddressWriter;

    public FreeMacSelector(final String caption, final MachineService machineService, final StateService stateService) {
        setCaption(caption);
        final ListSelect<MacAddress> listSelect = new ListSelect<>();

        listSelect.setItemCaptionGenerator(item -> item.toString());
        listSelect.setRows(0);
        final Collection<MacAddress> items = new TreeSet<>(Comparator.comparing(a -> a.getAddress()));
        final ListDataProvider<MacAddress> dataProvider = DataProvider.ofCollection(items);
        listSelect.setDataProvider(dataProvider);
        final Set<MacAddress> minimumAddressList = new HashSet<>();
        final Runnable updateListener = () -> {
            final Set<MacAddress> selection = listSelect.getValue();
            items.clear();
            items.addAll(minimumAddressList);
            items.addAll(machineService.listFreeMacs());
            listSelect.setValue(selection);
            dataProvider.refreshAll();
            if (items.size() < 8) {
                listSelect.setRows(items.size());
            } else {
                listSelect.setRows(8);
            }
        };
        final Disposable registration = stateService.registerForUpdates(() -> getUI().access(updateListener));
        addDetachListener(event -> registration.dispose());
        addAttachListener(event -> updateListener.run());
        macAddressReader = () -> listSelect.getValue();
        macAddressWriter = value -> {
            minimumAddressList.clear();
            minimumAddressList.addAll(value);
            listSelect.setValue(value);
            updateListener.run();
        };
        listSelect.setSizeFull();
        setCompositionRoot(listSelect);
    }

    public Set<MacAddress> getValue() {
        return macAddressReader.get();
    }

    public void setValue(final Set<MacAddress> value) {
        macAddressWriter.accept(value);
    }

}
