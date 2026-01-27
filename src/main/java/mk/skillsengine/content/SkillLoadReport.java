package mk.skillsengine.content;

import java.util.ArrayList;
import java.util.List;

public class SkillLoadReport {
    private int loaded;
    private int skipped;
    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();

    public void incLoaded() { loaded++; }
    public int loaded() { return loaded; }

    public void incSkipped() { skipped++; }
    public int skipped() { return skipped; }

    public void addError(String error) { errors.add(error); }
    public List<String> errors() { return errors; }

    public void addWarning(String warning) { warnings.add(warning); }
    public List<String> warnings() { return warnings; }
}
