package mk.skillsengine.engine;

import mk.skillsengine.model.Skill;

import java.util.*;

public class SkillRegistry {
    private final Map<String, Skill> byId = new HashMap<>();

    public void clear() {
        byId.clear();
    }

    public void register(Skill skill) {
        byId.put(skill.id().toLowerCase(Locale.ROOT), skill);
    }

    public Skill get(String id) {
        if (id == null) return null;
        return byId.get(id.toLowerCase(Locale.ROOT));
    }

    public Collection<Skill> all() {
        return Collections.unmodifiableCollection(byId.values());
    }
}
