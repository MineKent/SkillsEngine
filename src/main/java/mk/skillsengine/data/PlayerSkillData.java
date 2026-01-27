package mk.skillsengine.data;

import java.util.HashMap;
import java.util.Map;

public class PlayerSkillData {
    private final Map<String, Long> cooldownUntil = new HashMap<>();

    public long getCooldownUntil(String skillIdLower) {
        return cooldownUntil.getOrDefault(skillIdLower, 0L);
    }

    public void setCooldownUntil(String skillIdLower, long untilEpochMs) {
        cooldownUntil.put(skillIdLower, untilEpochMs);
    }

    public Map<String, Long> getAllCooldowns() {
        return cooldownUntil;
    }
}
