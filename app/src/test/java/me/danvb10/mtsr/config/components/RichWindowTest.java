package me.danvb10.mtsr.config.components;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the plain-state behaviour of {@link RichWindow}: default field
 * values, fluent setters and the child accumulator. These exercise only logic
 * that does not require a running Minecraft client, so {@code build()} and
 * {@code toggleMaximized} (which touch {@code MinecraftClient}) are out of scope.
 */
class RichWindowTest {

    private RichWindow newWindow() {
        return new RichWindow(null, RichWindowTypes.GENERAL_SETTINGS_WINDOW);
    }

    @Test
    void defaultsAreSetByConstructor() {
        RichWindow window = newWindow();

        assertEquals("Default", window.getWindowName());
        assertEquals("Default", window.getWindowTooltip());
        assertFalse(window.isFullHeight());
        assertFalse(window.isMaximized());
        assertFalse(window.isMinimizeIsDisabled());
    }

    @Test
    void setWindowNameUpdatesValueAndReturnsSameInstance() {
        RichWindow window = newWindow();

        RichWindow returned = window.setWindowName("General Settings");

        assertSame(window, returned);
        assertEquals("General Settings", window.getWindowName());
    }

    @Test
    void setWindowTooltipUpdatesValueAndReturnsSameInstance() {
        RichWindow window = newWindow();

        RichWindow returned = window.setWindowTooltip("Some tooltip");

        assertSame(window, returned);
        assertEquals("Some tooltip", window.getWindowTooltip());
    }

    @Test
    void setFullHeightTogglesFlagAndReturnsSameInstance() {
        RichWindow window = newWindow();

        assertSame(window, window.setFullHeight(true));
        assertTrue(window.isFullHeight());

        assertSame(window, window.setFullHeight(false));
        assertFalse(window.isFullHeight());
    }

    @Test
    void setMaximizedTogglesFlagAndReturnsSameInstance() {
        RichWindow window = newWindow();

        assertSame(window, window.setMaximized(true));
        assertTrue(window.isMaximized());

        assertSame(window, window.setMaximized(false));
        assertFalse(window.isMaximized());
    }

    @Test
    void setMinimizeIsDisabledTogglesFlagAndReturnsSameInstance() {
        RichWindow window = newWindow();

        assertSame(window, window.setMinimizeIsDisabled(true));
        assertTrue(window.isMinimizeIsDisabled());

        assertSame(window, window.setMinimizeIsDisabled(false));
        assertFalse(window.isMinimizeIsDisabled());
    }

    @Test
    void fluentSettersCanBeChained() {
        RichWindow window = newWindow()
                .setWindowName("Texture Manager")
                .setWindowTooltip("Manage textures")
                .setFullHeight(true)
                .setMaximized(true)
                .setMinimizeIsDisabled(true);

        assertEquals("Texture Manager", window.getWindowName());
        assertEquals("Manage textures", window.getWindowTooltip());
        assertTrue(window.isFullHeight());
        assertTrue(window.isMaximized());
        assertTrue(window.isMinimizeIsDisabled());
    }

    @Test
    void childReturnsSameInstanceForChaining() {
        RichWindow window = newWindow();

        assertSame(window, window.child(null));
    }
}
