package mk.skillsengine.util;

import java.util.logging.Logger;

public class Debug {
    private final Logger logger;
    private volatile boolean enabled;

    public Debug(Logger logger, boolean enabled) {
        this.logger = logger;
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void log(String msg) {
        if (!enabled) return;
        logger.info("[DEBUG] " + msg);
    }
}
