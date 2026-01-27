package mk.skillsengine.content;

import mk.skillsengine.model.Skill;
import mk.skillsengine.model.TargetType;
import mk.skillsengine.model.TriggerType;
import mk.skillsengine.util.ConfigUtil;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Validates skill config and returns human-readable errors/warnings.
 */
public final class SkillValidator {

    public record ValidationResult(List<String> errors, List<String> warnings) {
        public boolean ok() {
            return errors == null || errors.isEmpty();
        }
    }

    public ValidationResult validate(Skill skill) {
        var errors = new java.util.ArrayList<String>();
        var warnings = new java.util.ArrayList<String>();

        if (skill.id() == null || skill.id().isBlank()) {
            errors.add("id: missing");
        }

        // trigger validation
        if (skill.trigger() == null) {
            errors.add("trigger.type: missing");
        } else {
            validateTrigger(skill.trigger(), skill.triggerData(), errors, warnings);
        }

        // target validation
        if (skill.target() == null) {
            errors.add("target.type: missing");
        } else {
            validateTarget(skill.target(), skill.targetData(), errors, warnings);
        }

        // conditions
        int ci = 0;
        for (Map<String, Object> cond : skill.conditions()) {
            String type = ConfigUtil.getString(cond, "type", "").toUpperCase(Locale.ROOT);
            if (type.isBlank()) {
                warnings.add("conditions[" + ci + "].type: blank");
                ci++;
                continue;
            }
            switch (type) {
                case "COOLDOWN_READY" -> warnings.add("conditions[" + ci + "]: COOLDOWN_READY is deprecated (cooldown is checked automatically)");
                case "HAS_PERMISSION" -> {
                    String p = ConfigUtil.getString(cond, "permission", "");
                    if (p.isBlank()) errors.add("conditions[" + ci + "].permission: missing");
                }
                case "LEVEL_AT_LEAST" -> {
                    int min = ConfigUtil.getInt(cond, "min", ConfigUtil.getInt(cond, "level", -1));
                    if (min < 0) errors.add("conditions[" + ci + "].min: missing");
                }
                case "HAS_ITEM" -> {
                    String matRaw = ConfigUtil.getString(cond, "material", "");
                    if (matRaw.isBlank()) errors.add("conditions[" + ci + "].material: missing");
                    else if (Material.matchMaterial(matRaw) == null) errors.add("conditions[" + ci + "].material: bad_material=" + matRaw);
                }
                case "WORLD_ALLOWED" -> {
                    // optional lists
                }
                default -> errors.add("conditions[" + ci + "].type: unknown=" + type);
            }
            ci++;
        }

        // actions
        int ai = 0;
        for (Map<String, Object> act : skill.actions()) {
            String type = ConfigUtil.getString(act, "type", "").toUpperCase(Locale.ROOT);
            if (type.isBlank()) {
                warnings.add("actions[" + ai + "].type: blank");
                ai++;
                continue;
            }

            switch (type) {
                case "DAMAGE", "HEAL", "DASH" -> {
                    // optional amounts/power
                }
                case "POTION" -> {
                    String effect = ConfigUtil.getString(act, "effect", "");
                    if (effect.isBlank()) errors.add("actions[" + ai + "].effect: missing");
                    else if (PotionEffectType.getByName(effect.toUpperCase(Locale.ROOT)) == null) {
                        errors.add("actions[" + ai + "].effect: bad_effect=" + effect);
                    }
                }
                case "PARTICLES" -> {
                    String particle = ConfigUtil.getString(act, "particle", "");
                    if (!particle.isBlank()) {
                        try {
                            Particle.valueOf(particle.toUpperCase(Locale.ROOT));
                        } catch (IllegalArgumentException ex) {
                            errors.add("actions[" + ai + "].particle: bad_particle=" + particle);
                        }
                    }
                }
                case "SOUND" -> {
                    String sound = ConfigUtil.getString(act, "sound", "");
                    if (!sound.isBlank()) {
                        try {
                            Sound.valueOf(sound.toUpperCase(Locale.ROOT));
                        } catch (IllegalArgumentException ex) {
                            errors.add("actions[" + ai + "].sound: bad_sound=" + sound);
                        }
                    }
                }
                case "MESSAGE", "ACTIONBAR", "TITLE" -> {
                    // optional; nothing required
                }
                case "COMMAND" -> {
                    String cmd = ConfigUtil.getString(act, "command", "");
                    if (cmd.isBlank()) errors.add("actions[" + ai + "].command: missing");
                }
                case "TELEPORT" -> {
                    String to = ConfigUtil.getString(act, "to", "CENTER");
                    if (to.isBlank()) warnings.add("actions[" + ai + "].to: blank");
                }
                case "KNOCKBACK", "PULL", "SET_FIRE", "EXPLOSION" -> {
                    // optional
                }
                case "GIVE_ITEM", "TAKE_ITEM" -> {
                    String mat = ConfigUtil.getString(act, "material", "");
                    if (mat.isBlank()) errors.add("actions[" + ai + "].material: missing");
                    else if (Material.matchMaterial(mat) == null) errors.add("actions[" + ai + "].material: bad_material=" + mat);
                }
                case "SPAWN_ENTITY" -> {
                    String ent = ConfigUtil.getString(act, "entity", "");
                    if (ent.isBlank()) errors.add("actions[" + ai + "].entity: missing");
                    else {
                        try {
                            EntityType.valueOf(ent.toUpperCase(Locale.ROOT));
                        } catch (IllegalArgumentException ex) {
                            errors.add("actions[" + ai + "].entity: bad_entity=" + ent);
                        }
                    }
                }
                default -> errors.add("actions[" + ai + "].type: unknown=" + type);
            }

            ai++;
        }

        // cooldown sanity
        if (skill.cooldownMillis() < 0) warnings.add("cooldown: negative");

        return new ValidationResult(errors, warnings);
    }

    private void validateTrigger(TriggerType type, Map<String, Object> data, List<String> errors, List<String> warnings) {
        if (type == TriggerType.COMMAND) {
            String cmd = ConfigUtil.getString(data, "command", "").trim();
            if (cmd.isBlank()) errors.add("trigger.command: missing");
        }
        if (type == TriggerType.RIGHT_CLICK || type == TriggerType.LEFT_CLICK) {
            String mat = ConfigUtil.getString(data, "material", "");
            if (!mat.isBlank() && Material.matchMaterial(mat) == null) {
                errors.add("trigger.material: bad_material=" + mat);
            }
        }
    }

    private void validateTarget(TargetType type, Map<String, Object> data, List<String> errors, List<String> warnings) {
        if (type == TargetType.TARGET) {
            double range = ConfigUtil.getDouble(data, "range", -1);
            if (range <= 0) warnings.add("target.range: not set or <=0 (default will be used)");
        }
        if (type == TargetType.AREA) {
            double radius = ConfigUtil.getDouble(data, "radius", -1);
            if (radius <= 0) warnings.add("target.radius: not set or <=0 (default will be used)");
        }
    }
}
