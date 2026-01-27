package mk.skillsengine.engine;

import mk.skillsengine.util.ConfigUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Map;

public class CostApplier {

    /**
     * MVP cost: xpLevels, material+amount.
     */
    public SkillResult apply(Player player, Map<String, Object> cost) {
        if (cost == null || cost.isEmpty()) return SkillResult.ok();

        int xpLevels = ConfigUtil.getInt(cost, "xpLevels", 0);
        if (xpLevels > 0) {
            if (player.getLevel() < xpLevels) return SkillResult.fail("COST_XP_LEVELS");
        }

        String matRaw = ConfigUtil.getString(cost, "material", "");
        int amount = Math.max(0, ConfigUtil.getInt(cost, "amount", 0));
        Material mat = matRaw.isBlank() ? null : Material.matchMaterial(matRaw);
        if (mat != null && amount > 0) {
            if (!player.getInventory().containsAtLeast(new org.bukkit.inventory.ItemStack(mat), amount)) {
                return SkillResult.fail("COST_ITEM");
            }
        }

        // deduct after checks
        if (xpLevels > 0) {
            player.setLevel(player.getLevel() - xpLevels);
        }
        if (mat != null && amount > 0) {
            player.getInventory().removeItemAnySlot(new org.bukkit.inventory.ItemStack(mat, amount));
        }

        return SkillResult.ok();
    }
}
