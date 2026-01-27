package mk.skillsengine.trigger;

import mk.skillsengine.model.Skill;
import mk.skillsengine.model.TriggerType;
import mk.skillsengine.util.ConfigUtil;

import java.util.*;

/**
 * Indexes skills by trigger configuration for fast dispatch.
 */
public class TriggerIndex {
    private final Map<String, List<Skill>> commandToSkills = new HashMap<>();
    private final EnumMap<TriggerType, List<Skill>> byType = new EnumMap<>(TriggerType.class);

    public TriggerIndex() {
        for (TriggerType tt : TriggerType.values()) {
            byType.put(tt, new ArrayList<>());
        }
    }

    public void rebuild(Collection<Skill> skills) {
        commandToSkills.clear();
        for (TriggerType tt : TriggerType.values()) {
            byType.get(tt).clear();
        }

        for (Skill skill : skills) {
            byType.get(skill.trigger()).add(skill);
            if (skill.trigger() == TriggerType.COMMAND) {
                String cmd = ConfigUtil.getString(skill.triggerData(), "command", "").trim().toLowerCase(Locale.ROOT);
                if (!cmd.isBlank()) {
                    commandToSkills.computeIfAbsent(cmd, k -> new ArrayList<>()).add(skill);
                }
            }
        }
    }

    public List<Skill> forCommand(String commandLower) {
        return commandToSkills.getOrDefault(commandLower, List.of());
    }

    public List<Skill> forType(TriggerType type) {
        return byType.getOrDefault(type, List.of());
    }
}
