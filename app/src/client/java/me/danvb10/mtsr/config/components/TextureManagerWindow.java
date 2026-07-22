package me.danvb10.mtsr.config.components;

import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.core.UIComponent;
import me.danvb10.mtsr.config.ConfigScreen;

import static me.danvb10.mtsr.config.components.RichWindowTypes.*;

public class TextureManagerWindow {
    private final RichWindow richWindow;
    private boolean fullscreen;

    public TextureManagerWindow(ConfigScreen parent) {
        this.richWindow = new RichWindow(parent, TEXTURE_MANAGER_WINDOW);
    }

    public UIComponent build() {
        richWindow
                .setWindowName("Texture Manager")
                .setWindowTooltip("Texture Manager")
                .setFullHeight(true)
                .setMinimizeIsDisabled(true);

        if (fullscreen) richWindow.setMaximized(true);

        richWindow
                .child(UIComponents.label(net.minecraft.network.chat.Component.literal("a child")));

        return richWindow.build();
    }

    // Getters & Setters
    public boolean isFullscreen() {
        return fullscreen;
    }
    public TextureManagerWindow setFullscreen(boolean fullscreen) {
        this.fullscreen = fullscreen;
        return this;
    }
}
