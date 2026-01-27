package mk.skillsengine.util;

import java.util.Locale;

public final class EnumUtil {
    private EnumUtil() {}

    public static <E extends Enum<E>> E safeValueOf(Class<E> enumClass, Object raw, E def) {
        if (raw == null) return def;
        String s = String.valueOf(raw).trim();
        if (s.isEmpty()) return def;
        try {
            return Enum.valueOf(enumClass, s.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return def;
        }
    }
}
