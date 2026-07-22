package me.danvb10.mtsr.config.components;

import me.danvb10.mtsr.config.ConfigScreen;

import static me.danvb10.mtsr.config.components.RichWindowTypes.ACTIVITY_MONITOR_WINDOW;

public class ActivityLogWindow extends AbstractRichWindow<ActivityLogWindow> {

    // NOTE: behaviour preserved from before the refactor. This window is wired to
    // ACTIVITY_MONITOR_WINDOW / "Activity Monitor" (rather than ACTIVITY_LOG_WINDOW /
    // "Activity Log"), which appears to be a copy-paste bug in the original code.
    public ActivityLogWindow(ConfigScreen parent) {
        super(parent, ACTIVITY_MONITOR_WINDOW);
    }

    @Override
    protected String windowName() {
        return "Activity Monitor";
    }

    @Override
    protected boolean dockedFullHeight() {
        return true;
    }
}
