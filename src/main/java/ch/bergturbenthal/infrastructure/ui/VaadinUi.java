package ch.bergturbenthal.infrastructure.ui;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Widgetset;
import com.vaadin.server.VaadinRequest;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.spring.navigator.SpringNavigator;
import com.vaadin.ui.UI;

@SpringUI
@Theme("valo")
@Push
@Widgetset("ch.bergturbenthal.infrastructure.BootManagerWidgetSet")
public class VaadinUi extends UI {
    @Autowired
    private SpringNavigator springNavigator;

    @Override
    protected void init(final VaadinRequest vaadinRequest) {
        springNavigator.init(this, this);
        setNavigator(springNavigator);
        // ...
    }
}
