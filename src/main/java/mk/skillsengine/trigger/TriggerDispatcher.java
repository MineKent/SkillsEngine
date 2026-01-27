package mk.skillsengine.trigger;

import mk.skillsengine.engine.CastRequest;
import mk.skillsengine.engine.SkillEngineService;
import mk.skillsengine.model.Skill;
import mk.skillsengine.model.TriggerType;
import mk.skillsengine.util.ConfigUtil;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Locale;

/**
 * Trigger dispatcher using TriggerIndex for fast lookup.
 */
public class TriggerDispatcher implements Listener {
    private final TriggerIndex index;
    private final SkillEngineService engine;
    private final mk.skillsengine.util.Debug debug;

    public TriggerDispatcher(TriggerIndex index, SkillEngineService engine, mk.skillsengine.util.Debug debug) {
        this.index = index;
        this.engine = engine;
        this.debug = debug;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        String msg = e.getMessage();
        String cmd = msg.startsWith("/") ? msg.substring(1) : msg;
        String base = cmd.split("\\s+")[0].toLowerCase(Locale.ROOT);

        var skills = index.forCommand(base);
        if (skills.isEmpty()) return;

        // prevent recursion / conflict by cancelling actual command
        e.setCancelled(true);
        for (Skill skill : skills) {
            engine.cast(e.getPlayer(), skill.id(), new CastRequest().setInitialLocation(e.getPlayer().getLocation()));
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        Action a = e.getAction();
        if (debug != null) debug.log("PlayerInteractEvent action=" + a + " cancelled=" + e.isCancelled() + " player=" + e.getPlayer().getName());
        boolean right = a == Action.RIGHT_CLICK_AIR || a == Action.RIGHT_CLICK_BLOCK;
        boolean left = a == Action.LEFT_CLICK_AIR || a == Action.LEFT_CLICK_BLOCK;
        if (!right && !left) return;

        TriggerType tt = right ? TriggerType.RIGHT_CLICK : TriggerType.LEFT_CLICK;
        Player p = e.getPlayer();

        ItemStack inHand = p.getInventory().getItemInMainHand();

        var skills = index.forType(tt);
        if (debug != null) debug.log("dispatch " + tt + ": skills=" + skills.size());
        for (Skill skill : skills) {
            // optional filter: trigger.material
            String matRaw = ConfigUtil.getString(skill.triggerData(), "material", "");
            if (!matRaw.isBlank()) {
                Material mat = Material.matchMaterial(matRaw);
                if (mat == null) continue;
                if (inHand == null || inHand.getType() != mat) continue;
            }
            engine.cast(p, skill.id(), new CastRequest().setInitialLocation(p.getLocation()));
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p)) return;
        LivingEntity hit = (e.getEntity() instanceof LivingEntity le) ? le : null;
        for (Skill skill : index.forType(TriggerType.ON_HIT)) {
            engine.cast(p, skill.id(), new CastRequest().setInitialTarget(hit).setTriggeringEntity(e.getEntity()));
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        LivingEntity damager = null;
        if (e instanceof EntityDamageByEntityEvent ebe && ebe.getDamager() instanceof LivingEntity le) {
            damager = le;
        }
        for (Skill skill : index.forType(TriggerType.ON_DAMAGE)) {
            engine.cast(p, skill.id(), new CastRequest().setInitialTarget(damager).setTriggeringEntity(damager));
        }
    }
}
