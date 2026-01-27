package mk.skillsengine.util;

import mk.skillsengine.engine.SkillContext;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class MessageFormatter {
    private MessageFormatter() {}

    public static String colorize(String s) {
        if (s == null) return null;
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public static String applyPlaceholders(String template, Player caster, String skillId, Map<String, String> vars) {
        if (template == null) return null;
        String out = template;
        if (caster != null) {
            out = out.replace("{player}", caster.getName());
        }
        if (skillId != null) {
            out = out.replace("{skill}", skillId);
        }
        if (vars != null) {
            for (var e : vars.entrySet()) {
                if (e.getKey() == null || e.getValue() == null) continue;
                out = out.replace("{" + e.getKey() + "}", e.getValue());
            }
        }
        return colorize(out);
    }

    /**
     * Parses reason like "COOLDOWN:5s" into {code=COOLDOWN, payload=5s}.
     */
    public static Map<String, String> parseReasonVars(String reason) {
        if (reason == null) return Map.of();
        int idx = reason.indexOf(':');
        if (idx <= 0) return Map.of("reason", reason, "code", reason);
        String code = reason.substring(0, idx).toUpperCase(Locale.ROOT);
        String payload = reason.substring(idx + 1);

        Map<String, String> vars = new HashMap<>();
        vars.put("reason", reason);
        vars.put("code", code);
        vars.put("payload", payload);

        // common payload conventions
        if (code.equals("COOLDOWN")) {
            vars.put("seconds", payload.replace("s", ""));
        }
        return vars;
    }
}
