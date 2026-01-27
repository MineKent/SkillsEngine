package mk.skillsengine.engine;

import mk.skillsengine.data.PlayerDataStore;
import mk.skillsengine.data.PlayerSkillData;
import mk.skillsengine.model.Skill;
import org.bukkit.entity.Player;

import java.util.Locale;

public class SkillEngineService {
    private final SkillRegistry registry;
    private final PlayerDataStore dataStore;
    private final ConditionEvaluator conditionEvaluator;
    private final CostApplier costApplier;
    private final LevelProvider levelProvider;
    private final mk.skillsengine.util.Debug debug;

    private final java.util.function.Function<String, String> defaultMessageResolver;

    private final TargetResolver targetResolver = new TargetResolver();
    private final ActionExecutor actionExecutor = new ActionExecutor();
    public SkillEngineService(
            SkillRegistry registry,
            PlayerDataStore dataStore,
            ConditionEvaluator conditionEvaluator,
            CostApplier costApplier,
            LevelProvider levelProvider,
            mk.skillsengine.util.Debug debug,
            java.util.function.Function<String, String> messageByKey
    ) {
        this.registry = registry;
        this.dataStore = dataStore;
        this.conditionEvaluator = conditionEvaluator;
        this.costApplier = costApplier;
        this.levelProvider = levelProvider;
        this.debug = debug;

        // messageByKey("cooldown") -> template
        this.defaultMessageResolver = messageByKey;
    }

    public SkillResult cast(Player caster, String skillId) {
        return cast(caster, skillId, null);
    }

    public SkillResult cast(Player caster, String skillId, CastRequest request) {
        Skill skill = registry.get(skillId);
        if (skill == null) return SkillResult.fail("UNKNOWN_SKILL");
        if (debug != null) debug.log("cast request: player=" + caster.getName() + " skill=" + skill.id() + " trigger=" + skill.trigger() + " target=" + skill.target());

        PlayerSkillData psd = dataStore.get(caster.getUniqueId());

        // cooldown is always enforced if configured
        if (skill.cooldownMillis() > 0) {
            long now = System.currentTimeMillis();
            long until = psd.getCooldownUntil(skill.id().toLowerCase(Locale.ROOT));
            if (until > now) {
                long leftMs = until - now;
                long leftSec = (long) Math.ceil(leftMs / 1000.0);
                SkillResult r = SkillResult.fail("COOLDOWN:" + leftSec + "s");
                sendDenyMessage(caster, skill, r);
                return r;
            }
        }

        // conditions
        SkillResult cond = conditionEvaluator.checkAll(caster, skill, psd, levelProvider);
        if (!cond.success()) {
            if (debug != null) debug.log("cast denied by condition: skill=" + skill.id() + " reason=" + cond.reason());
            sendDenyMessage(caster, skill, cond);
            return cond;
        }

        // cost
        SkillResult costRes = costApplier.apply(caster, skill.cost());
        if (!costRes.success()) {
            if (debug != null) debug.log("cast denied by cost: skill=" + skill.id() + " reason=" + costRes.reason());
            sendDenyMessage(caster, skill, costRes);
            return costRes;
        }

        SkillContext ctx = new SkillContext(skill, caster);
        if (request != null) {
            ctx.setTargetEntity(request.getInitialTarget());
            ctx.setTargetLocation(request.getInitialLocation());
            ctx.setTriggeringEntity(request.getTriggeringEntity());
        }

        // targeting
        SkillResult targetRes = targetResolver.resolve(ctx);
        if (!targetRes.success()) {
            if (debug != null) debug.log("cast denied by targeting: skill=" + skill.id() + " reason=" + targetRes.reason());
            sendDenyMessage(caster, skill, targetRes);
            return targetRes;
        }

        // actions
        SkillResult actionRes = actionExecutor.executeAll(ctx);
        if (!actionRes.success()) {
            if (debug != null) debug.log("cast failed in actions: skill=" + skill.id() + " reason=" + actionRes.reason());
            sendDenyMessage(caster, skill, actionRes);
            return actionRes;
        }

        // start cooldown after successful cast
        if (skill.cooldownMillis() > 0) {
            long until = System.currentTimeMillis() + skill.cooldownMillis();
            psd.setCooldownUntil(skill.id().toLowerCase(Locale.ROOT), until);
        }

        if (debug != null) debug.log("cast OK: skill=" + skill.id());
        return SkillResult.ok();
    }

    private void sendDenyMessage(Player caster, Skill skill, SkillResult result) {
        if (caster == null || skill == null || result == null || result.success()) return;

        // 1) skill-level override
        if (skill.denyMessage() != null && !skill.denyMessage().isBlank()) {
            var vars = mk.skillsengine.util.MessageFormatter.parseReasonVars(result.reason());
            String msg = mk.skillsengine.util.MessageFormatter.applyPlaceholders(skill.denyMessage(), caster, skill.id(), vars);
            caster.sendMessage(msg);
            return;
        }

        // 2) global templates by reason code
        var vars = mk.skillsengine.util.MessageFormatter.parseReasonVars(result.reason());
        String code = vars.getOrDefault("code", "");

        String key = switch (code) {
            case "COOLDOWN" -> "cooldown";
            case "NO_PERMISSION" -> "no_permission";
            case "LOW_LEVEL" -> "low_level";
            case "MISSING_ITEM" -> "missing_item";
            case "COST_XP_LEVELS" -> "cost_xp_levels";
            case "COST_ITEM" -> "cost_item";
            case "NO_TARGET" -> "no_target";
            case "WORLD_NOT_ALLOWED" -> "world_not_allowed";
            case "WORLD_DENIED" -> "world_denied";
            default -> "default";
        };

        String template = defaultMessageResolver != null ? defaultMessageResolver.apply(key) : null;
        if (template == null || template.isBlank()) {
            template = "Cannot cast {skill}: {reason}";
        }

        String msg = mk.skillsengine.util.MessageFormatter.applyPlaceholders(template, caster, skill.id(), vars);
        caster.sendMessage(msg);
    }
}
