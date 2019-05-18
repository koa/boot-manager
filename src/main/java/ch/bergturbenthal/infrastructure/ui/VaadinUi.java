package ch.bergturbenthal.infrastructure.ui;

import com.vaadin.annotations.Push;
import com.vaadin.annotations.Widgetset;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.mpr.core.HasLegacyComponents;

import javax.annotation.PostConstruct;

@Route
@Push
@Widgetset("ch.bergturbenthal.infrastructure.BootManagerWidgetSet")
public class VaadinUi extends Div implements HasLegacyComponents, RouterLayout {

  @PostConstruct
  protected void buildLayouts() {

  }
}
