package ch.bergturbenthal.infrastructure.ui.view;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;

import com.vaadin.data.provider.DataProvider;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.navigator.View;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

import ch.bergturbenthal.infrastructure.event.RemovePatternEvent;
import ch.bergturbenthal.infrastructure.event.UpdatePatternEvent;
import ch.bergturbenthal.infrastructure.service.LogService;
import ch.bergturbenthal.infrastructure.service.PatternService;
import ch.bergturbenthal.infrastructure.service.StateService;
import reactor.core.Disposable;

@SpringView(name = "patterns")
public class PatternEditor extends CustomComponent implements View {
    public PatternEditor(final StateService stateService, final PatternService patternService, final LogService logService) {

        final ComboBox<String> comboBox = new ComboBox<>("select Pattern");
        comboBox.setNewItemHandler(string -> {
            logService.appendEvent(new UpdatePatternEvent(string, ""));
            comboBox.setValue(string);
        });
        final TextArea contentArea = new TextArea("content");
        final AtomicReference<String> originalContent = new AtomicReference<String>("");

        final Runnable switchText = () -> {
            final Map<String, String> patterns = patternService.listPatterns();
            final String newText = patterns.getOrDefault(comboBox.getValue(), "");
            originalContent.set(newText);
            contentArea.setValue(newText);
        };
        comboBox.addValueChangeListener(event -> {
            final String currentText = contentArea.getValue();
            if (event.isUserOriginated() && !currentText.equals(originalContent.get())) {
                final Window window = new Window("are you sure?");
                window.setModal(true);
                final Label questionLabel = new Label("Content has modified");
                final Button cancelButton = new Button("abort", clickEv -> {
                    comboBox.setValue(event.getOldValue());
                    window.close();
                });
                final Button ignoreButton = new Button("ingore changes", clickEv -> {
                    switchText.run();
                    window.close();
                });
                final Button saveButton = new Button("saveChanges", clickEv -> {
                    logService.appendEvent(new UpdatePatternEvent(event.getOldValue(), contentArea.getValue()));
                    window.close();
                });
                window.setContent(new VerticalLayout(questionLabel, new HorizontalLayout(cancelButton, ignoreButton, saveButton)));
                window.center();
                getUI().addWindow(window);
            } else {
                switchText.run();
            }
        });
        final Button saveButton = new Button("update", event -> {
            logService.appendEvent(new UpdatePatternEvent(comboBox.getValue(), contentArea.getValue()));
        });
        final Button deleteButton = new Button("delete", event -> {
            logService.appendEvent(new RemovePatternEvent(comboBox.getValue()));
        });
        final Set<String> patternList = new TreeSet<>();
        final ListDataProvider<String> dataProvider = DataProvider.ofCollection(patternList);
        comboBox.setDataProvider(dataProvider);
        final Runnable updateData = () -> {
            final Map<String, String> patterns = patternService.listPatterns();
            final String currentSelection = Optional.ofNullable(comboBox.getValue()).orElseGet(() -> {
                final Set<String> availablePatterns = patterns.keySet();
                if (availablePatterns.isEmpty()) {
                    return null;
                }
                return availablePatterns.iterator().next();
            });
            patternList.clear();
            if (currentSelection != null) {
                patternList.add(currentSelection);
            }
            patternList.addAll(patterns.keySet());
            dataProvider.refreshAll();
            if (currentSelection != null && patternList.contains(currentSelection)) {
                comboBox.setValue(currentSelection);
                final String storedText = patterns.get(currentSelection);
                if (storedText != null && !storedText.equals(originalContent.get())) {
                    switchText.run();
                }
            }

        };
        contentArea.setSizeFull();
        final VerticalLayout mainLayout = new VerticalLayout(comboBox, contentArea, new HorizontalLayout(saveButton, deleteButton));
        mainLayout.setExpandRatio(contentArea, 1);
        mainLayout.setSizeFull();
        setCompositionRoot(mainLayout);
        setSizeFull();
        final Disposable registration = stateService.registerForUpdates(() -> getUI().access(updateData));
        addDetachListener(event -> registration.dispose());
        addAttachListener(event -> updateData.run());
    }
}
