package me.danvb10.mtsr.config.components;

import me.danvb10.mtsr.config.ConfigScreen;

import static me.danvb10.mtsr.config.components.RichWindowTypes.QUICK_ACTIONS_WINDOW;

public class QuickActionsWindow extends AbstractRichWindow<QuickActionsWindow> {

    public QuickActionsWindow(ConfigScreen parent) {
        super(parent, QUICK_ACTIONS_WINDOW);
    }

    @Override
    protected String windowName() {
        return "Quick Actions";
    }
}
