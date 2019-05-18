package ch.bergturbenthal.infrastructure.event;

import lombok.Value;

import javax.validation.constraints.NotNull;
import java.util.Comparator;
import java.util.Objects;

@Value
public class RedirectBootAction implements BootAction {
  private String redirectTarget;

  @Override
  public int compareTo(final @NotNull BootAction o) {
    if (o == null)
      return -1;
    if (o instanceof RedirectBootAction)
      return Objects.compare(redirectTarget, ((RedirectBootAction) o).getRedirectTarget(), Comparator.naturalOrder());
    return o.getClass().getName().compareTo(
            getClass().getName());
  }
}
