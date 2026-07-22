package me.danvb10.mtsr.config.components;

import me.danvb10.mtsr.config.ConfigScreen;

import static me.danvb10.mtsr.config.components.RichWindowTypes.MODEL_SETTINGS_WINDOW;

public class ModelSettingsWindow extends AbstractRichWindow<ModelSettingsWindow> {

    public ModelSettingsWindow(ConfigScreen parent) {
        super(parent, MODEL_SETTINGS_WINDOW);
    }

    @Override
    protected String windowName() {
        return "Model Settings";
    }
}
