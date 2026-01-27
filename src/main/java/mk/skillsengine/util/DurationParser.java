package mk.skillsengine.util;

import java.util.Locale;

public final class DurationParser {
    private DurationParser() {}

    /**
     * Parses strings like "500ms", "10s", "2m". If value is purely numeric -> seconds.
     */
    public static long parseToMillis(Object raw, long defMillis) {
        if (raw == null) return defMillis;
        if (raw instanceof Number n) {
            // treat as seconds for YAML friendliness
            return Math.round(n.doubleValue() * 1000.0);
        }
        String s = String.valueOf(raw).trim().toLowerCase(Locale.ROOT);
        if (s.isEmpty()) return defMillis;

        try {
            if (s.endsWith("ms")) {
                return Long.parseLong(s.substring(0, s.length() - 2).trim());
            }
            if (s.endsWith("s")) {
                double v = Double.parseDouble(s.substring(0, s.length() - 1).trim());
                return Math.round(v * 1000.0);
            }
            if (s.endsWith("m")) {
                double v = Double.parseDouble(s.substring(0, s.length() - 1).trim());
                return Math.round(v * 60_000.0);
            }
            if (s.endsWith("h")) {
                double v = Double.parseDouble(s.substring(0, s.length() - 1).trim());
                return Math.round(v * 3_600_000.0);
            }
            // numeric without suffix -> seconds
            double v = Double.parseDouble(s);
            return Math.round(v * 1000.0);
        } catch (NumberFormatException ignored) {
            return defMillis;
        }
    }
}
