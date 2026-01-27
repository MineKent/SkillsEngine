package mk.skillsengine.data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * In-memory player data store.
 *
 * Note: data is NOT persisted to disk (no players.yml). It resets on server restart.
 */
public class PlayerDataStore {
    private final Map<UUID, PlayerSkillData> data = new HashMap<>();

    public PlayerSkillData get(UUID uuid) {
        return data.computeIfAbsent(uuid, k -> new PlayerSkillData());
    }

    public void clear() {
        data.clear();
    }
}
