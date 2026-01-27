package mk.skillsengine.engine;

import mk.skillsengine.util.ConfigUtil;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ActionExecutor {

    public SkillResult executeAll(SkillContext ctx) {
        for (Map<String, Object> action : ctx.skill().actions()) {
            String type = ConfigUtil.getString(action, "type", "").toUpperCase(Locale.ROOT);
            if (type.isBlank()) continue;

            SkillResult res = switch (type) {
                case "DAMAGE" -> damage(ctx, action);
                case "HEAL" -> heal(ctx, action);
                case "POTION" -> potion(ctx, action);
                case "DASH" -> dash(ctx, action);
                case "PARTICLES" -> particles(ctx, action);
                case "SOUND" -> sound(ctx, action);

                case "MESSAGE" -> message(ctx, action);
                case "ACTIONBAR" -> actionbar(ctx, action);
                case "TITLE" -> title(ctx, action);
                case "COMMAND" -> command(ctx, action);
                case "TELEPORT" -> teleport(ctx, action);
                case "KNOCKBACK" -> knockback(ctx, action, false);
                case "PULL" -> knockback(ctx, action, true);
                case "SET_FIRE" -> setFire(ctx, action);
                case "EXPLOSION" -> explosion(ctx, action);
                case "GIVE_ITEM" -> giveItem(ctx, action);
                case "TAKE_ITEM" -> takeItem(ctx, action);
                case "SPAWN_ENTITY" -> spawnEntity(ctx, action);

                default -> SkillResult.fail("UNKNOWN_ACTION:" + type);
            };

            if (!res.success()) return res;
        }
        return SkillResult.ok();
    }

    private List<? extends LivingEntity> targets(SkillContext ctx) {
        return ctx.resolvedTargets() == null ? List.of() : ctx.resolvedTargets();
    }

    private SkillResult damage(SkillContext ctx, Map<String, Object> action) {
        double amount = ConfigUtil.getDouble(action, "amount", 1.0);
        for (LivingEntity le : targets(ctx)) {
            le.damage(amount, ctx.caster());
        }
        return SkillResult.ok();
    }

    private SkillResult heal(SkillContext ctx, Map<String, Object> action) {
        double amount = ConfigUtil.getDouble(action, "amount", 1.0);
        for (LivingEntity le : targets(ctx)) {
            double max = le.getAttribute(Attribute.MAX_HEALTH) != null ? le.getAttribute(Attribute.MAX_HEALTH).getValue() : 20.0;
            le.setHealth(Math.min(max, le.getHealth() + amount));
        }
        return SkillResult.ok();
    }

    private SkillResult potion(SkillContext ctx, Map<String, Object> action) {
        String effectRaw = ConfigUtil.getString(action, "effect", "");
        if (effectRaw.isBlank()) return SkillResult.fail("POTION:missing_effect");
        PotionEffectType type = PotionEffectType.getByName(effectRaw.toUpperCase(Locale.ROOT));
        if (type == null) return SkillResult.fail("POTION:bad_effect:" + effectRaw);

        int durationTicks = ConfigUtil.getInt(action, "durationTicks", 20 * ConfigUtil.getInt(action, "duration", 5));
        int amplifier = Math.max(0, ConfigUtil.getInt(action, "amplifier", 0));
        boolean ambient = ConfigUtil.getBool(action, "ambient", false);
        boolean particles = ConfigUtil.getBool(action, "particles", true);
        boolean icon = ConfigUtil.getBool(action, "icon", true);

        PotionEffect pe = new PotionEffect(type, durationTicks, amplifier, ambient, particles, icon);
        for (LivingEntity le : targets(ctx)) {
            le.addPotionEffect(pe);
        }
        return SkillResult.ok();
    }

    private SkillResult dash(SkillContext ctx, Map<String, Object> action) {
        Player p = ctx.caster();
        double power = ConfigUtil.getDouble(action, "power", 1.2);
        boolean keepY = ConfigUtil.getBool(action, "keepY", true);

        Vector dir = p.getLocation().getDirection().normalize().multiply(power);
        if (keepY) dir.setY(p.getVelocity().getY());
        p.setVelocity(dir);
        return SkillResult.ok();
    }

    private SkillResult particles(SkillContext ctx, Map<String, Object> action) {
        String particleRaw = ConfigUtil.getString(action, "particle", "");
        if (particleRaw.isBlank()) return SkillResult.ok();

        Location loc = resolveLocation(ctx, action);

        Particle particle;
        try {
            particle = Particle.valueOf(particleRaw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return SkillResult.fail("PARTICLES:bad_particle:" + particleRaw);
        }

        int count = Math.max(0, ConfigUtil.getInt(action, "count", 10));
        double offset = ConfigUtil.getDouble(action, "offset", 0.2);
        double offsetX = ConfigUtil.getDouble(action, "offsetX", offset);
        double offsetY = ConfigUtil.getDouble(action, "offsetY", offset);
        double offsetZ = ConfigUtil.getDouble(action, "offsetZ", offset);
        double speed = ConfigUtil.getDouble(action, "speed", 0);
        boolean showAllPlayers = ConfigUtil.getBool(action, "show_all_players", false);

        // Extended: support REDSTONE dust options
        Object data = null;
        if (particle == Particle.DUST || particle == Particle.DUST_COLOR_TRANSITION) {
            Color from = parseColor(action, "color", Color.RED);
            float size = (float) ConfigUtil.getDouble(action, "size", 1.0);
            if (particle == Particle.DUST) {
                data = new Particle.DustOptions(from, size);
            } else {
                Color to = parseColor(action, "toColor", Color.WHITE);
                data = new Particle.DustTransition(from, to, size);
            }
        }

        if (count <= 0) {
            // allow count=0 no-op
            return SkillResult.ok();
        }

        if (data != null) {
            ctx.caster().getWorld().spawnParticle(particle, loc, count, offsetX, offsetY, offsetZ, speed, data, showAllPlayers);
        } else {
            ctx.caster().getWorld().spawnParticle(particle, loc, count, offsetX, offsetY, offsetZ, speed, null, showAllPlayers);
        }

        return SkillResult.ok();
    }

    private SkillResult sound(SkillContext ctx, Map<String, Object> action) {
        String soundRaw = ConfigUtil.getString(action, "sound", "");
        if (soundRaw.isBlank()) return SkillResult.ok();

        Sound sound;
        try {
            sound = Sound.valueOf(soundRaw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return SkillResult.fail("SOUND:bad_sound:" + soundRaw);
        }

        float volume = (float) ConfigUtil.getDouble(action, "volume", 1.0);
        float pitch = (float) ConfigUtil.getDouble(action, "pitch", 1.0);

        SoundCategory category = null;
        String catRaw = ConfigUtil.getString(action, "category", "");
        if (!catRaw.isBlank()) {
            try {
                category = SoundCategory.valueOf(catRaw.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return SkillResult.fail("SOUND:bad_category:" + catRaw);
            }
        }

        String mode = ConfigUtil.getString(action, "mode", "WORLD").toUpperCase(Locale.ROOT);
        Location loc = resolveLocation(ctx, action);

        switch (mode) {
            case "CASTER" -> {
                if (category != null) ctx.caster().playSound(loc, sound, category, volume, pitch);
                else ctx.caster().playSound(loc, sound, volume, pitch);
            }
            case "TARGETS" -> {
                for (LivingEntity le : targets(ctx)) {
                    if (le instanceof Player pl) {
                        if (category != null) pl.playSound(loc, sound, category, volume, pitch);
                        else pl.playSound(loc, sound, volume, pitch);
                    }
                }
            }
            case "WORLD" -> {
                if (category != null) ctx.caster().getWorld().playSound(loc, sound, category, volume, pitch);
                else ctx.caster().getWorld().playSound(loc, sound, volume, pitch);
            }
            default -> {
                return SkillResult.fail("SOUND:bad_mode:" + mode);
            }
        }

        return SkillResult.ok();
    }

    private Location resolveLocation(SkillContext ctx, Map<String, Object> action) {
        String at = ConfigUtil.getString(action, "at", "");
        if (!at.isBlank()) {
            String a = at.toUpperCase(Locale.ROOT);
            if (a.equals("CASTER")) return ctx.caster().getLocation();
            if (a.equals("TARGET") && ctx.targetEntity() != null) return ctx.targetEntity().getLocation();
            if (a.equals("CENTER") && ctx.targetLocation() != null) return ctx.targetLocation();
        }
        // default: best-known target location or caster location
        return ctx.targetLocation() != null ? ctx.targetLocation() : ctx.caster().getLocation();
    }

    private Color parseColor(Map<String, Object> action, String key, Color def) {
        Object raw = action.get(key);
        if (raw == null) return def;
        String s = String.valueOf(raw).trim();
        try {
            if (s.startsWith("#")) {
                int rgb = Integer.parseInt(s.substring(1), 16);
                return Color.fromRGB(rgb);
            }
            if (s.contains(",")) {
                String[] parts = s.split(",");
                if (parts.length >= 3) {
                    int r = Integer.parseInt(parts[0].trim());
                    int g = Integer.parseInt(parts[1].trim());
                    int b = Integer.parseInt(parts[2].trim());
                    return Color.fromRGB(clamp(r), clamp(g), clamp(b));
                }
            }
            // allow raw int
            int rgb = Integer.parseInt(s);
            return Color.fromRGB(rgb);
        } catch (Exception ignored) {
            return def;
        }
    }

    private int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    private String formatText(SkillContext ctx, String raw) {
        if (raw == null) return "";
        String s = raw;
        s = s.replace("{player}", ctx.caster().getName());
        s = s.replace("{skill}", ctx.skill().id());
        if (ctx.targetEntity() != null) {
            s = s.replace("{target}", ctx.targetEntity().getName());
        }
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    private SkillResult message(SkillContext ctx, Map<String, Object> action) {
        String text = ConfigUtil.getString(action, "text", ConfigUtil.getString(action, "message", ""));
        if (text.isBlank()) return SkillResult.ok();
        String mode = ConfigUtil.getString(action, "mode", "CHAT").toUpperCase(Locale.ROOT);
        String msg = formatText(ctx, text);

        switch (mode) {
            case "CHAT" -> ctx.caster().sendMessage(msg);
            case "ACTIONBAR" -> ctx.caster().sendActionBar(msg);
            case "TITLE" -> ctx.caster().sendTitle(msg, "", 10, 40, 10);
            default -> {
                return SkillResult.fail("MESSAGE:bad_mode:" + mode);
            }
        }
        return SkillResult.ok();
    }

    private SkillResult actionbar(SkillContext ctx, Map<String, Object> action) {
        String text = ConfigUtil.getString(action, "text", ConfigUtil.getString(action, "message", ""));
        if (text.isBlank()) return SkillResult.ok();
        ctx.caster().sendActionBar(formatText(ctx, text));
        return SkillResult.ok();
    }

    private SkillResult title(SkillContext ctx, Map<String, Object> action) {
        String title = ConfigUtil.getString(action, "title", ConfigUtil.getString(action, "text", ""));
        String subtitle = ConfigUtil.getString(action, "subtitle", "");
        int fadeIn = Math.max(0, ConfigUtil.getInt(action, "fadeIn", 10));
        int stay = Math.max(0, ConfigUtil.getInt(action, "stay", 40));
        int fadeOut = Math.max(0, ConfigUtil.getInt(action, "fadeOut", 10));
        if (title.isBlank() && subtitle.isBlank()) return SkillResult.ok();
        ctx.caster().sendTitle(formatText(ctx, title), formatText(ctx, subtitle), fadeIn, stay, fadeOut);
        return SkillResult.ok();
    }

    private SkillResult command(SkillContext ctx, Map<String, Object> action) {
        String cmdRaw = ConfigUtil.getString(action, "command", "");
        if (cmdRaw.isBlank()) return SkillResult.fail("COMMAND:missing_command");
        String executor = ConfigUtil.getString(action, "executor", "CONSOLE").toUpperCase(Locale.ROOT);

        CommandSender sender;
        switch (executor) {
            case "CONSOLE" -> sender = Bukkit.getConsoleSender();
            case "PLAYER" -> sender = ctx.caster();
            default -> {
                return SkillResult.fail("COMMAND:bad_executor:" + executor);
            }
        }

        String cmd = formatText(ctx, cmdRaw);
        if (cmd.startsWith("/")) cmd = cmd.substring(1);
        boolean ok = Bukkit.dispatchCommand(sender, cmd);
        return ok ? SkillResult.ok() : SkillResult.fail("COMMAND:dispatch_failed");
    }

    private SkillResult teleport(SkillContext ctx, Map<String, Object> action) {
        String to = ConfigUtil.getString(action, "to", "CENTER").toUpperCase(Locale.ROOT);
        Location loc;
        switch (to) {
            case "CASTER" -> loc = ctx.caster().getLocation();
            case "TARGET" -> {
                if (ctx.targetEntity() == null) return SkillResult.fail("TELEPORT:no_target");
                loc = ctx.targetEntity().getLocation();
            }
            case "CENTER" -> {
                loc = ctx.targetLocation();
                if (loc == null) loc = ctx.caster().getLocation();
            }
            case "LOCATION" -> {
                String worldName = ConfigUtil.getString(action, "world", "");
                World w = worldName.isBlank() ? ctx.caster().getWorld() : Bukkit.getWorld(worldName);
                if (w == null) return SkillResult.fail("TELEPORT:bad_world:" + worldName);
                double x = ConfigUtil.getDouble(action, "x", ctx.caster().getLocation().getX());
                double y = ConfigUtil.getDouble(action, "y", ctx.caster().getLocation().getY());
                double z = ConfigUtil.getDouble(action, "z", ctx.caster().getLocation().getZ());
                float yaw = (float) ConfigUtil.getDouble(action, "yaw", ctx.caster().getLocation().getYaw());
                float pitch = (float) ConfigUtil.getDouble(action, "pitch", ctx.caster().getLocation().getPitch());
                loc = new Location(w, x, y, z, yaw, pitch);
            }
            default -> {
                return SkillResult.fail("TELEPORT:bad_to:" + to);
            }
        }

        boolean ok = ctx.caster().teleport(loc);
        return ok ? SkillResult.ok() : SkillResult.fail("TELEPORT:failed");
    }

    private SkillResult knockback(SkillContext ctx, Map<String, Object> action, boolean pull) {
        double strength = ConfigUtil.getDouble(action, "strength", ConfigUtil.getDouble(action, "power", 1.0));
        double y = ConfigUtil.getDouble(action, "y", 0.35);

        for (LivingEntity le : targets(ctx)) {
            if (le == null) continue;
            Vector dir;
            if (pull) {
                dir = ctx.caster().getLocation().toVector().subtract(le.getLocation().toVector());
            } else {
                dir = le.getLocation().toVector().subtract(ctx.caster().getLocation().toVector());
            }
            if (dir.lengthSquared() < 0.0001) continue;
            dir.normalize().multiply(strength);
            dir.setY(y);
            le.setVelocity(dir);
        }
        return SkillResult.ok();
    }

    private SkillResult setFire(SkillContext ctx, Map<String, Object> action) {
        int ticks = Math.max(0, ConfigUtil.getInt(action, "ticks", 20 * ConfigUtil.getInt(action, "seconds", 3)));
        for (LivingEntity le : targets(ctx)) {
            le.setFireTicks(Math.max(le.getFireTicks(), ticks));
        }
        return SkillResult.ok();
    }

    private SkillResult explosion(SkillContext ctx, Map<String, Object> action) {
        Location loc = resolveLocation(ctx, action);
        float power = (float) ConfigUtil.getDouble(action, "power", 2.0);
        boolean setFire = ConfigUtil.getBool(action, "setFire", false);
        boolean breakBlocks = ConfigUtil.getBool(action, "breakBlocks", false);
        boolean damageEntities = ConfigUtil.getBool(action, "damageEntities", true);

        // Paper/Spigot variants differ; keep it simple & compatible.
        if (!damageEntities) {
            // Create explosion without damage is not consistently supported; emulate by zero power
            loc.getWorld().createExplosion(loc.getX(), loc.getY(), loc.getZ(), 0.0f, setFire, breakBlocks);
            return SkillResult.ok();
        }

        loc.getWorld().createExplosion(loc.getX(), loc.getY(), loc.getZ(), power, setFire, breakBlocks, ctx.caster());
        return SkillResult.ok();
    }

    private SkillResult giveItem(SkillContext ctx, Map<String, Object> action) {
        String matRaw = ConfigUtil.getString(action, "material", "");
        if (matRaw.isBlank()) return SkillResult.fail("GIVE_ITEM:missing_material");
        Material mat = Material.matchMaterial(matRaw);
        if (mat == null) return SkillResult.fail("GIVE_ITEM:bad_material:" + matRaw);
        int amount = Math.max(1, ConfigUtil.getInt(action, "amount", 1));

        ItemStack stack = new ItemStack(mat, amount);
        ctx.caster().getInventory().addItem(stack);
        return SkillResult.ok();
    }

    private SkillResult takeItem(SkillContext ctx, Map<String, Object> action) {
        String matRaw = ConfigUtil.getString(action, "material", "");
        if (matRaw.isBlank()) return SkillResult.fail("TAKE_ITEM:missing_material");
        Material mat = Material.matchMaterial(matRaw);
        if (mat == null) return SkillResult.fail("TAKE_ITEM:bad_material:" + matRaw);
        int amount = Math.max(1, ConfigUtil.getInt(action, "amount", 1));

        ctx.caster().getInventory().removeItemAnySlot(new ItemStack(mat, amount));
        return SkillResult.ok();
    }

    private SkillResult spawnEntity(SkillContext ctx, Map<String, Object> action) {
        String entityRaw = ConfigUtil.getString(action, "entity", "");
        if (entityRaw.isBlank()) return SkillResult.fail("SPAWN_ENTITY:missing_entity");

        EntityType type;
        try {
            type = EntityType.valueOf(entityRaw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return SkillResult.fail("SPAWN_ENTITY:bad_entity:" + entityRaw);
        }

        int count = Math.max(1, ConfigUtil.getInt(action, "count", 1));
        Location loc = resolveLocation(ctx, action);

        for (int i = 0; i < count; i++) {
            try {
                loc.getWorld().spawnEntity(loc, type);
            } catch (Exception ex) {
                return SkillResult.fail("SPAWN_ENTITY:failed:" + ex.getMessage());
            }
        }
        return SkillResult.ok();
    }
}
