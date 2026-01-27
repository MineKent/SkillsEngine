package mk.skillsengine.engine;

import mk.skillsengine.data.PlayerSkillData;
import mk.skillsengine.model.Skill;
import mk.skillsengine.util.ConfigUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class ConditionEvaluator {

    private List<String> listOfStrings(Object o) {
        if (!(o instanceof List<?> l)) return null;
        return l.stream().filter(Objects::nonNull).map(String::valueOf).toList();
    }

    public SkillResult checkAll(Player caster, Skill skill, PlayerSkillData playerData, LevelProvider levelProvider) {
        for (Map<String, Object> cond : skill.conditions()) {
            String type = ConfigUtil.getString(cond, "type", "").toUpperCase(Locale.ROOT);
            if (type.isBlank()) continue;

            SkillResult res = switch (type) {
                // Deprecated: cooldown is now always enforced when skill.cooldownMillis() > 0
                case "COOLDOWN_READY" -> SkillResult.ok();
                case "HAS_PERMISSION" -> checkPermission(caster, cond);
                case "LEVEL_AT_LEAST" -> checkLevel(caster, cond, playerData, levelProvider);
                case "HAS_ITEM" -> checkHasItem(caster, cond);
                case "WORLD_ALLOWED" -> checkWorld(caster, cond);
                default -> SkillResult.fail("UNKNOWN_CONDITION:" + type);
            };

            if (!res.success()) return res;
        }
        return SkillResult.ok();
    }

    private SkillResult checkPermission(Player caster, Map<String, Object> cond) {
        String perm = ConfigUtil.getString(cond, "permission", "");
        if (perm.isBlank()) return SkillResult.ok();
        if (!caster.hasPermission(perm)) return SkillResult.fail("NO_PERMISSION");
        return SkillResult.ok();
    }

    private SkillResult checkLevel(Player caster, Map<String, Object> cond, PlayerSkillData data, LevelProvider provider) {
        int min = ConfigUtil.getInt(cond, "min", ConfigUtil.getInt(cond, "level", 0));
        int lvl = provider.getLevel(caster);
        if (lvl < min) return SkillResult.fail("LOW_LEVEL:" + lvl + "<" + min);
        return SkillResult.ok();
    }

    private SkillResult checkHasItem(Player caster, Map<String, Object> cond) {
        String materialRaw = ConfigUtil.getString(cond, "material", "");
        if (materialRaw.isBlank()) return SkillResult.fail("HAS_ITEM:material_missing");
        Material mat = Material.matchMaterial(materialRaw);
        if (mat == null) return SkillResult.fail("HAS_ITEM:bad_material:" + materialRaw);

        int amount = Math.max(1, ConfigUtil.getInt(cond, "amount", 1));
        String where = ConfigUtil.getString(cond, "where", "INVENTORY").toUpperCase(Locale.ROOT);

        if (where.equals("HAND") || where.equals("MAIN_HAND")) {
            ItemStack inHand = caster.getInventory().getItemInMainHand();
            if (inHand.getType() != mat || inHand.getAmount() < amount) return SkillResult.fail("MISSING_ITEM");
            return SkillResult.ok();
        }

        int count = 0;
        for (ItemStack it : caster.getInventory().getContents()) {
            if (it == null) continue;
            if (it.getType() != mat) continue;
            count += it.getAmount();
            if (count >= amount) break;
        }
        if (count < amount) return SkillResult.fail("MISSING_ITEM");
        return SkillResult.ok();
    }

    private SkillResult checkWorld(Player caster, Map<String, Object> cond) {
        World w = caster.getWorld();
        String worldName = w.getName();
        List<String> allowed = listOfStrings(cond.get("allowed"));
        List<String> denied = listOfStrings(cond.get("denied"));

        if (allowed != null && !allowed.isEmpty()) {
            boolean ok = allowed.stream().anyMatch(s -> s != null && s.equalsIgnoreCase(worldName));
            if (!ok) return SkillResult.fail("WORLD_NOT_ALLOWED");
        }
        if (denied != null && !denied.isEmpty()) {
            boolean bad = denied.stream().anyMatch(s -> s != null && s.equalsIgnoreCase(worldName));
            if (bad) return SkillResult.fail("WORLD_DENIED");
        }
        return SkillResult.ok();
    }
}
