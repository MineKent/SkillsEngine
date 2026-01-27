package mk.skillsengine.command;

import mk.skillsengine.engine.SkillRegistry;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SkillsEngineTabCompleter implements TabCompleter {
    private final SkillRegistry registry;

    public SkillsEngineTabCompleter(SkillRegistry registry) {
        this.registry = registry;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("reload", "cast"), args[0]);
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("cast")) {
                List<String> ids = registry.all().stream().map(s -> s.id()).toList();
                return filter(ids, args[1]);
            }
        }
        if (args.length == 3) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("cast")) {
                List<String> names = Bukkit.getOnlinePlayers().stream().map(p -> p.getName()).toList();
                return filter(names, args[2]);
            }
        }
        return List.of();
    }

    private List<String> filter(List<String> values, String prefix) {
        String p = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String v : values) {
            if (v == null) continue;
            if (v.toLowerCase(Locale.ROOT).startsWith(p)) out.add(v);
        }
        return out;
    }
}
