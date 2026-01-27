package mk.skillsengine.model;

import java.util.List;
import java.util.Map;

public record Skill(
        String id,
        String name,
        String type,
        TriggerType trigger,
        Map<String, Object> triggerData,
        TargetType target,
        Map<String, Object> targetData,
        List<Map<String, Object>> conditions,
        List<Map<String, Object>> actions,
        long cooldownMillis,
        Map<String, Object> cost,
        String denyMessage
) {
}
