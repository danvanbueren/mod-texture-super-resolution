package me.danvb10.mtsr.config.components;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextureManagerWindowTest {

    @Test
    void windowNameReturnsTextureManager() {
        TextureManagerWindow window = new TextureManagerWindow(null);
        assertEquals("Texture Manager", window.windowName());
    }

    @Test
    void dockedFullHeightIsTrue() {
        TextureManagerWindow window = new TextureManagerWindow(null);
        assertTrue(window.dockedFullHeight());
    }
}
