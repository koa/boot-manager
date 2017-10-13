package ch.bergturbenthal.infrastructure.ui.view;

import java.util.Collection;
import java.util.Comparator;
import java.util.TreeSet;

import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.navigator.View;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.ListSelect;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import ch.bergturbenthal.infrastructure.model.MacAddress;
import ch.bergturbenthal.infrastructure.service.MachineService;
import reactor.core.Disposable;

@SpringView(name = "")
public class FreeMacView extends VerticalLayout implements View {

    public FreeMacView(final MachineService machineService) {

        final ListSelect<MacAddress> listSelect = new ListSelect<>("available macs");
        listSelect.setItemCaptionGenerator(item -> item.toString());
        final Collection<MacAddress> items = new TreeSet<>(Comparator.comparing(a -> a.getAddress()));
        final ListDataProvider<MacAddress> dataProvider = DataProvider.ofCollection(items);
        listSelect.setDataProvider(dataProvider);
        final Runnable updateListener = () -> runOnUi(() -> {
            items.clear();
            items.addAll(machineService.listFreeMacs());
            dataProvider.refreshAll();
        });
        final Disposable registration = machineService.registerForUpdates(updateListener);
        addComponent(listSelect);
        addDetachListener(event -> registration.dispose());
        addAttachListener(event -> updateListener.run());
    }

    private void runOnUi(final Runnable runnable) {
        final UI ui = getUI();
        if (ui == null) {
            runnable.run();
        } else {
            ui.access(runnable);
        }
    }

}
