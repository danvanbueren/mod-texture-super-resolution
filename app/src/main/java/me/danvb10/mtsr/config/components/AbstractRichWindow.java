package me.danvb10.mtsr.config.components;

import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.core.Component;
import me.danvb10.mtsr.config.ConfigScreen;
import net.minecraft.text.Text;

/**
 * Shared base for the small window wrappers around {@link RichWindow}. Each concrete window only
 * needs to supply its {@link RichWindowTypes}, a display name and (optionally) whether it should be
 * rendered as a docked, full-height panel.
 *
 * @param <SELF> the concrete subtype, used so fluent setters keep returning the subtype
 */
public abstract class AbstractRichWindow<SELF extends AbstractRichWindow<SELF>> {

    protected final RichWindow richWindow;
    private boolean fullscreen;

    protected AbstractRichWindow(ConfigScreen parent, RichWindowTypes windowType) {
        this.richWindow = new RichWindow(parent, windowType);
    }

    // Display name shown in the title bar.
    protected abstract String windowName();

    // Tooltip for the title bar; defaults to the window name.
    protected String windowTooltip() {
        return windowName();
    }

    // When true the window is always full-height with minimize disabled (a docked panel).
    protected boolean dockedFullHeight() {
        return false;
    }

    public Component build() {
        richWindow
                .setWindowName(windowName())
                .setWindowTooltip(windowTooltip());

        if (dockedFullHeight()) {
            richWindow
                    .setFullHeight(true)
                    .setMinimizeIsDisabled(true);
            if (fullscreen) richWindow.setMaximized(true);
        } else if (fullscreen) {
            richWindow
                    .setFullHeight(true)
                    .setMaximized(true)
                    .setMinimizeIsDisabled(true);
        }

        richWindow.child(Components.label(Text.literal("a child")));

        return richWindow.build();
    }

    // Getters & Setters
    public boolean isFullscreen() {
        return fullscreen;
    }

    @SuppressWarnings("unchecked")
    public SELF setFullscreen(boolean fullscreen) {
        this.fullscreen = fullscreen;
        return (SELF) this;
    }
}
