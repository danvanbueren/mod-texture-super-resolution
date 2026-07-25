package me.danvb10.mtsr.config.components;

import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.core.UIComponent;
import me.danvb10.mtsr.config.ConfigScreen;
import net.minecraft.network.chat.Component;

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

    // Adds the window's body components; defaults to a placeholder label.
    protected void populateContent(RichWindow window) {
        window.child(UIComponents.label(Component.literal("a child")));
    }

    public UIComponent build() {
        richWindow.clearChildren();
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

        populateContent(richWindow);

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

    protected void requestScreenRefresh() {
        net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
        if (client != null && client.screen instanceof me.danvb10.mtsr.config.RefreshableScreen refreshable) {
            refreshable.requestRefresh();
        }
    }
}
