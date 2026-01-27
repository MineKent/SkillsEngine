package mk.skillsengine.engine;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

/**
 * Extra context coming from triggers/events.
 */
public class CastRequest {
    private LivingEntity initialTarget;
    private Location initialLocation;
    private Entity triggeringEntity;

    public LivingEntity getInitialTarget() {
        return initialTarget;
    }

    public CastRequest setInitialTarget(LivingEntity initialTarget) {
        this.initialTarget = initialTarget;
        return this;
    }

    public Location getInitialLocation() {
        return initialLocation;
    }

    public CastRequest setInitialLocation(Location initialLocation) {
        this.initialLocation = initialLocation;
        return this;
    }

    public Entity getTriggeringEntity() {
        return triggeringEntity;
    }

    public CastRequest setTriggeringEntity(Entity triggeringEntity) {
        this.triggeringEntity = triggeringEntity;
        return this;
    }
}
