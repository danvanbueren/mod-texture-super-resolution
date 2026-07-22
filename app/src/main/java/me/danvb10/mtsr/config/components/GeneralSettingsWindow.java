package me.danvb10.mtsr.config.components;

import me.danvb10.mtsr.config.ConfigScreen;

import static me.danvb10.mtsr.config.components.RichWindowTypes.GENERAL_SETTINGS_WINDOW;

public class GeneralSettingsWindow extends AbstractRichWindow<GeneralSettingsWindow> {

    public GeneralSettingsWindow(ConfigScreen parent) {
        super(parent, GENERAL_SETTINGS_WINDOW);
    }

    @Override
    protected String windowName() {
        return "General Settings";
    }
}
