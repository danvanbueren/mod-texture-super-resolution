package me.danvb10.mtsr.config.components;

import me.danvb10.mtsr.config.ConfigScreen;

import static me.danvb10.mtsr.config.components.RichWindowTypes.ACTIVITY_MONITOR_WINDOW;

public class ActivityMonitorWindow extends AbstractRichWindow<ActivityMonitorWindow> {

    public ActivityMonitorWindow(ConfigScreen parent) {
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
