package me.danvb10.mtsr.config.components;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActivityLogWindowTest {

    @Test
    void windowNameReturnsActivityLog() {
        ActivityLogWindow window = new ActivityLogWindow(null);
        assertEquals("Activity Log", window.windowName());
    }

    @Test
    void dockedFullHeightIsTrue() {
        ActivityLogWindow window = new ActivityLogWindow(null);
        assertTrue(window.dockedFullHeight());
    }
}
