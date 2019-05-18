package ch.bergturbenthal.infrastructure.event;

import lombok.Value;

import java.util.Comparator;
import java.util.Objects;

@Value
public class PatternBootAction implements BootAction {
  private String patternName;

  @Override
  public int compareTo(final BootAction o) {
    if (o == null)
      return -1;
    if (o instanceof RedirectBootAction)
      return Objects.compare(patternName, ((PatternBootAction) o).getPatternName(), Comparator.naturalOrder());
    return o.getClass().getName().compareTo(
            getClass().getName());

  }
}
