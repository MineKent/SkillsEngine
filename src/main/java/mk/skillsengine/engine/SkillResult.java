package mk.skillsengine.engine;

public record SkillResult(boolean success, String reason) {
    public static SkillResult ok() {
        return new SkillResult(true, null);
    }

    public static SkillResult fail(String reason) {
        return new SkillResult(false, reason);
    }
}
