package mk.skillsengine;

import mk.skillsengine.command.SkillsEngineCommand;
import mk.skillsengine.content.SkillLoader;
import mk.skillsengine.engine.SkillEngineService;
import mk.skillsengine.engine.SkillRegistry;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class SkillsEngine extends JavaPlugin {

    private SkillRegistry skillRegistry;
    private SkillLoader skillLoader;
    private SkillEngineService engineService;

    private mk.skillsengine.data.PlayerDataStore dataStore;

    private File skillsFolder;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        skillRegistry = new SkillRegistry();
        skillsFolder = new File(getDataFolder(), "skills");

        // In-memory only (no players.yml persistence)
        dataStore = new mk.skillsengine.data.PlayerDataStore();

        var debug = new mk.skillsengine.util.Debug(getLogger(), getConfig().getBoolean("debug", false));
        var levelProvider = new mk.skillsengine.engine.MinecraftLevelProvider();

        engineService = new SkillEngineService(
                skillRegistry,
                dataStore,
                new mk.skillsengine.engine.ConditionEvaluator(),
                new mk.skillsengine.engine.CostApplier(),
                levelProvider,
                debug,
                (key) -> getConfig().getString("messages." + key, "")
        );

        var triggerIndex = new mk.skillsengine.trigger.TriggerIndex();
        skillLoader = new SkillLoader(this, skillRegistry, triggerIndex);

        saveDefaultSkillExamples();
        skillLoader.reloadAll(skillsFolder);

        PluginCommand cmd = getCommand("se");
        if (cmd != null) {
            cmd.setExecutor(new SkillsEngineCommand(skillLoader, engineService, skillsFolder));
            cmd.setTabCompleter(new mk.skillsengine.command.SkillsEngineTabCompleter(skillRegistry));
        }

        getServer().getPluginManager().registerEvents(
                new mk.skillsengine.trigger.TriggerDispatcher(triggerIndex, engineService, debug),
                this
        );
    }

    private void saveDefaultSkillExamples() {
        if (!getDataFolder().exists()) {
            //noinspection ResultOfMethodCallIgnored
            getDataFolder().mkdirs();
        }
        if (!skillsFolder.exists()) {
            //noinspection ResultOfMethodCallIgnored
            skillsFolder.mkdirs();
        }

        // create default skill examples/templates
        File example = new File(skillsFolder, "example.yml");
        if (!example.exists()) {
            saveResource("skills/example.yml", false);
        }
        File template = new File(skillsFolder, "_template.yml");
        if (!template.exists()) {
            saveResource("skills/_template.yml", false);
        }
    }

    @Override
    public void onDisable() {
        // no-op: PlayerDataStore is in-memory only
    }
}
