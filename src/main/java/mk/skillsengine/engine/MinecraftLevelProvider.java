package mk.skillsengine.engine;

import org.bukkit.entity.Player;

public class MinecraftLevelProvider implements LevelProvider {
    @Override
    public int getLevel(Player player) {
        return player.getLevel();
    }
}
