package mk.skillsengine.content;

import mk.skillsengine.engine.SkillRegistry;
import mk.skillsengine.model.Skill;
import mk.skillsengine.model.TargetType;
import mk.skillsengine.model.TriggerType;
import mk.skillsengine.util.ConfigUtil;
import mk.skillsengine.util.DurationParser;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.*;

public class SkillLoader {
    private final Plugin plugin;
    private final SkillRegistry registry;
    private final mk.skillsengine.trigger.TriggerIndex triggerIndex;

    public SkillLoader(Plugin plugin, SkillRegistry registry, mk.skillsengine.trigger.TriggerIndex triggerIndex) {
        this.plugin = plugin;
        this.registry = registry;
        this.triggerIndex = triggerIndex;
    }

    public SkillLoadReport reloadAll(File skillsFolder) {
        SkillLoadReport report = new SkillLoadReport();
        registry.clear();

        SkillValidator validator = new SkillValidator();

        if (!skillsFolder.exists() && !skillsFolder.mkdirs()) {
            report.addError("Could not create folder: " + skillsFolder.getPath());
            return report;
        }

        File[] files = skillsFolder.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files == null) return report;

        Arrays.sort(files, Comparator.comparing(File::getName));
        for (File file : files) {
            try {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
                Skill skill = parseSkill(file, yaml);

                SkillValidator.ValidationResult vr = validator.validate(skill);
                for (String w : vr.warnings()) report.addWarning(file.getName() + ": " + w);
                if (!vr.ok()) {
                    for (String e : vr.errors()) report.addError(file.getName() + ": " + e);
                    report.incSkipped();
                    continue;
                }

                registry.register(skill);
                report.incLoaded();
            } catch (Exception ex) {
                plugin.getLogger().warning("[SkillsEngine] Failed to load skill file " + file.getName() + ": " + ex.getMessage());
                report.addError(file.getName() + ": " + ex.getMessage());
                report.incSkipped();
            }
        }

        triggerIndex.rebuild(registry.all());
        return report;
    }

    private Skill parseSkill(File file, YamlConfiguration yaml) {
        String fileBaseId = file.getName();
        if (fileBaseId.toLowerCase(Locale.ROOT).endsWith(".yml")) {
            fileBaseId = fileBaseId.substring(0, fileBaseId.length() - 4);
        }

        String id = yaml.getString("id", fileBaseId);
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Missing id");

        String name = yaml.getString("name", id);
        String type = yaml.getString("type", "DEFAULT");

        Map<String, Object> triggerMap = ConfigUtil.toMap(yaml.getConfigurationSection("trigger"));
        String triggerTypeRaw = ConfigUtil.getString(triggerMap, "type", "COMMAND");
        TriggerType triggerType = mk.skillsengine.util.EnumUtil.safeValueOf(TriggerType.class, triggerTypeRaw, TriggerType.COMMAND);

        Map<String, Object> targetMap = ConfigUtil.toMap(yaml.getConfigurationSection("target"));
        String targetTypeRaw = ConfigUtil.getString(targetMap, "type", "SELF");
        TargetType targetType = mk.skillsengine.util.EnumUtil.safeValueOf(TargetType.class, targetTypeRaw, TargetType.SELF);

        List<Map<String, Object>> conditions = ConfigUtil.listOfMaps(yaml.get("conditions"));
        List<Map<String, Object>> actions = ConfigUtil.listOfMaps(yaml.get("actions"));

        long cooldownMillis = DurationParser.parseToMillis(yaml.get("cooldown"), 0);
        Map<String, Object> cost = ConfigUtil.asMap(yaml.get("cost"));
        String denyMessage = yaml.getString("denyMessage", null);

        // remove 'type' key from trigger/target data? keep as-is but store separate too
        return new Skill(
                id,
                name,
                type,
                triggerType,
                triggerMap,
                targetType,
                targetMap,
                conditions,
                actions,
                cooldownMillis,
                cost,
                denyMessage
        );
    }
}
