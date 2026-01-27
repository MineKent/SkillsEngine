package mk.skillsengine.command;

import mk.skillsengine.content.SkillLoadReport;
import mk.skillsengine.content.SkillLoader;
import mk.skillsengine.engine.SkillEngineService;
import mk.skillsengine.engine.SkillResult;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.Locale;

public class SkillsEngineCommand implements CommandExecutor {
    private final SkillLoader loader;
    private final SkillEngineService engine;
    private final File skillsFolder;

    public SkillsEngineCommand(SkillLoader loader, SkillEngineService engine, File skillsFolder) {
        this.loader = loader;
        this.engine = engine;
        this.skillsFolder = skillsFolder;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("SkillsEngine commands:");
            sender.sendMessage(" /se reload");
            sender.sendMessage(" /se cast <skillId> [player]");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload" -> {
                SkillLoadReport report = loader.reloadAll(skillsFolder);
                sender.sendMessage("SkillsEngine: loaded " + report.loaded() + " skill(s), skipped " + report.skipped());
                for (String warn : report.warnings()) {
                    sender.sendMessage(" - warning: " + warn);
                }
                for (String err : report.errors()) {
                    sender.sendMessage(" - error: " + err);
                }
                return true;
            }
            case "cast" -> {
                if (args.length < 2) {
                    sender.sendMessage("Usage: /se cast <skillId> [player]");
                    return true;
                }
                String skillId = args[1];
                Player targetPlayer;
                if (args.length >= 3) {
                    targetPlayer = Bukkit.getPlayerExact(args[2]);
                    if (targetPlayer == null) {
                        sender.sendMessage("Player not found: " + args[2]);
                        return true;
                    }
                } else {
                    if (!(sender instanceof Player p)) {
                        sender.sendMessage("Console must specify player: /se cast <skillId> <player>");
                        return true;
                    }
                    targetPlayer = p;
                }

                SkillResult res = engine.cast(targetPlayer, skillId);
                sender.sendMessage("Cast " + skillId + ": " + (res.success() ? "OK" : ("FAIL: " + res.reason())));
                return true;
            }
            default -> {
                sender.sendMessage("Unknown subcommand. /se reload | /se cast <skillId>");
                return true;
            }
        }
    }
}
