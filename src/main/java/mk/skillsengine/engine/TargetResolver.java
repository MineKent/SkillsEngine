package mk.skillsengine.engine;

import mk.skillsengine.model.Skill;
import mk.skillsengine.model.TargetType;
import mk.skillsengine.util.ConfigUtil;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class TargetResolver {

    public SkillResult resolve(SkillContext ctx) {
        Skill skill = ctx.skill();
        Player caster = ctx.caster();

        TargetType tt = skill.target();
        switch (tt) {
            case SELF -> {
                ctx.setTargetEntity(caster);
                ctx.setTargetLocation(caster.getLocation());
                ctx.setResolvedTargets(List.of(caster));
                return SkillResult.ok();
            }
            case TARGET -> {
                // If trigger already provided a target (e.g., ON_HIT), prefer it.
                if (ctx.targetEntity() != null) {
                    LivingEntity le = ctx.targetEntity();
                    ctx.setTargetLocation(le.getLocation());
                    ctx.setResolvedTargets(List.of(le));
                    return SkillResult.ok();
                }

                double range = ConfigUtil.getDouble(skill.targetData(), "range", 4.5);
                Entity e = caster.getTargetEntity((int) Math.ceil(range));
                if (e instanceof LivingEntity le) {
                    ctx.setTargetEntity(le);
                    ctx.setTargetLocation(le.getLocation());
                    ctx.setResolvedTargets(List.of(le));
                    return SkillResult.ok();
                }
                return SkillResult.fail("NO_TARGET");
            }
            case AREA -> {
                double radius = ConfigUtil.getDouble(skill.targetData(), "radius", 3.0);
                String center = ConfigUtil.getString(skill.targetData(), "center", "CASTER");
                Location base = caster.getLocation();
                if (center.equalsIgnoreCase("TARGET") && ctx.targetEntity() != null) {
                    base = ctx.targetEntity().getLocation();
                }

                World w = base.getWorld();
                List<LivingEntity> targets = new ArrayList<>();
                for (Entity ent : w.getNearbyEntities(base, radius, radius, radius)) {
                    if (ent instanceof LivingEntity le) {
                        targets.add(le);
                    }
                }
                if (targets.isEmpty()) {
                    // AREA can still be valid with empty list; actions decide what to do.
                }
                ctx.setTargetLocation(base);
                ctx.setResolvedTargets(targets);
                return SkillResult.ok();
            }
            default -> {
                return SkillResult.fail("UNKNOWN_TARGET");
            }
        }
    }
}
