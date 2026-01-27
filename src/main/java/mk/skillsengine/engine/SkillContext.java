package mk.skillsengine.engine;

import mk.skillsengine.model.Skill;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.List;

public class SkillContext {
    private final Skill skill;
    private final Player caster;

    private LivingEntity targetEntity; // optional
    private Location targetLocation; // optional (for AREA centers etc)

    // resolved targets for actions (SELF/TARGET/AREA)
    private List<? extends LivingEntity> resolvedTargets;

    // trigger origin entity (for ON_HIT etc)
    private Entity triggeringEntity;

    public SkillContext(Skill skill, Player caster) {
        this.skill = skill;
        this.caster = caster;
    }

    public Skill skill() { return skill; }
    public Player caster() { return caster; }

    public LivingEntity targetEntity() { return targetEntity; }
    public void setTargetEntity(LivingEntity targetEntity) { this.targetEntity = targetEntity; }

    public Location targetLocation() { return targetLocation; }
    public void setTargetLocation(Location targetLocation) { this.targetLocation = targetLocation; }

    public List<? extends LivingEntity> resolvedTargets() { return resolvedTargets; }
    public void setResolvedTargets(List<? extends LivingEntity> resolvedTargets) { this.resolvedTargets = resolvedTargets; }

    public Entity triggeringEntity() { return triggeringEntity; }
    public void setTriggeringEntity(Entity triggeringEntity) { this.triggeringEntity = triggeringEntity; }
}
