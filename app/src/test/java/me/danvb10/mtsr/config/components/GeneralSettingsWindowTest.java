package me.danvb10.mtsr.config.components;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GeneralSettingsWindowTest {

    @Test
    void windowNameReturnsGeneralSettings() {
        GeneralSettingsWindow window = new GeneralSettingsWindow(null);
        assertEquals("General Settings", window.windowName());
    }

    @Test
    void dockedFullHeightIsFalse() {
        GeneralSettingsWindow window = new GeneralSettingsWindow(null);
        assertFalse(window.dockedFullHeight());
    }
}
