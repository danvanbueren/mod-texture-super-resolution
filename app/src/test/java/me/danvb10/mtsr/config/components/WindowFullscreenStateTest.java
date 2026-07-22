package me.danvb10.mtsr.config.components;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the {@code fullscreen} getter/setter contract shared by every config
 * window wrapper: it defaults to {@code false}, the setter mutates it and returns
 * the same (correctly typed) instance for fluent chaining. Constructors are given
 * a {@code null} owner because none of them dereference it, and {@code build()} is
 * intentionally not called (it requires a Minecraft client).
 */
class WindowFullscreenStateTest {

    @Test
    void generalSettingsWindow() {
        GeneralSettingsWindow window = new GeneralSettingsWindow(null);
        assertFalse(window.isFullscreen());
        assertSame(window, window.setFullscreen(true));
        assertTrue(window.isFullscreen());
        assertSame(window, window.setFullscreen(false));
        assertFalse(window.isFullscreen());
    }

    @Test
    void modelSettingsWindow() {
        ModelSettingsWindow window = new ModelSettingsWindow(null);
        assertFalse(window.isFullscreen());
        assertSame(window, window.setFullscreen(true));
        assertTrue(window.isFullscreen());
        assertSame(window, window.setFullscreen(false));
        assertFalse(window.isFullscreen());
    }

    @Test
    void quickActionsWindow() {
        QuickActionsWindow window = new QuickActionsWindow(null);
        assertFalse(window.isFullscreen());
        assertSame(window, window.setFullscreen(true));
        assertTrue(window.isFullscreen());
        assertSame(window, window.setFullscreen(false));
        assertFalse(window.isFullscreen());
    }

    @Test
    void textureManagerWindow() {
        TextureManagerWindow window = new TextureManagerWindow(null);
        assertFalse(window.isFullscreen());
        assertSame(window, window.setFullscreen(true));
        assertTrue(window.isFullscreen());
        assertSame(window, window.setFullscreen(false));
        assertFalse(window.isFullscreen());
    }

    @Test
    void activityMonitorWindow() {
        ActivityMonitorWindow window = new ActivityMonitorWindow(null);
        assertFalse(window.isFullscreen());
        assertSame(window, window.setFullscreen(true));
        assertTrue(window.isFullscreen());
        assertSame(window, window.setFullscreen(false));
        assertFalse(window.isFullscreen());
    }

    @Test
    void activityLogWindow() {
        ActivityLogWindow window = new ActivityLogWindow(null);
        assertFalse(window.isFullscreen());
        assertSame(window, window.setFullscreen(true));
        assertTrue(window.isFullscreen());
        assertSame(window, window.setFullscreen(false));
        assertFalse(window.isFullscreen());
    }
}
