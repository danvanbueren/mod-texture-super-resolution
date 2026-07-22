package me.danvb10.mtsr.config.components;

import me.danvb10.mtsr.config.ConfigScreen;

import static me.danvb10.mtsr.config.components.RichWindowTypes.TEXTURE_MANAGER_WINDOW;

public class TextureManagerWindow extends AbstractRichWindow<TextureManagerWindow> {

    public TextureManagerWindow(ConfigScreen parent) {
        super(parent, TEXTURE_MANAGER_WINDOW);
    }

    @Override
    protected String windowName() {
        return "Texture Manager";
    }

    @Override
    protected boolean dockedFullHeight() {
        return true;
    }
}
