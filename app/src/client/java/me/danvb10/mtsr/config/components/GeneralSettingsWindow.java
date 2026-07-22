package me.danvb10.mtsr.config.components;

import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.core.UIComponent;
import me.danvb10.mtsr.config.ConfigScreen;

import static me.danvb10.mtsr.config.components.RichWindowTypes.*;

public class GeneralSettingsWindow {
    private final RichWindow richWindow;
    private boolean fullscreen;

    public GeneralSettingsWindow(ConfigScreen parent) {
        this.richWindow = new RichWindow(parent, GENERAL_SETTINGS_WINDOW);
    }

    public UIComponent build() {
        richWindow
                .setWindowName("General Settings")
                .setWindowTooltip("General Settings");

        if (fullscreen) {
            richWindow
                    .setFullHeight(true)
                    .setMaximized(true)
                    .setMinimizeIsDisabled(true);
        }

        richWindow
                .child(UIComponents.label(net.minecraft.network.chat.Component.literal("a child")));

        return richWindow.build();
    }

    // Getters & Setters
    public boolean isFullscreen() {
        return fullscreen;
    }
    public GeneralSettingsWindow setFullscreen(boolean fullscreen) {
        this.fullscreen = fullscreen;
        return this;
    }
}
