package ch.bergturbenthal.infrastructure.event;

public class OnebootPatternScope implements PatternScope {

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        return getClass().equals(obj.getClass());
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String toString() {
        return "next Boot";
    }

}
