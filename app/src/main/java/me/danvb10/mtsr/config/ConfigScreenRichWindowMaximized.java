package me.danvb10.mtsr.config;

import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import static me.danvb10.mtsr.ClientEntrypoint.LOGGER;
import static me.danvb10.mtsr.config.ConfigScreen.getScreenTitle;

public class ConfigScreenRichWindowMaximized extends BaseOwoScreen<FlowLayout> {

    private final ArrayList<UIComponent> children;
    private final Screen parent;

    // Constructor
    public ConfigScreenRichWindowMaximized(Screen parent) {
        this.parent = parent;
        this.children = new ArrayList<>();
    }

    // Create adapter
    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, UIContainers::verticalFlow);
    }

    // Build full component
    @Override
    protected void build(FlowLayout root) {
        root
                .child(getScreenTitle(parent))
                .child(
                        UIContainers.horizontalFlow(Sizing.fill(100), Sizing.fill(94))
                                .child(
                                        UIContainers.verticalScroll(Sizing.fill(100), Sizing.fill(100),
                                                        UIContainers.verticalFlow(Sizing.fill(100), Sizing.content())
                                                                .child(
                                                                        UIContainers.verticalFlow(Sizing.fill(100), Sizing.content())
                                                                                .children(this.children)
                                                                                .padding(Insets.of(0, 0, 0, 8))
                                                                )
                                                )
                                                .scrollbarThiccness(6)
                                                .padding(Insets.of(0, 0, 0, 17))
                                )
                )
                .surface(Surface.VANILLA_TRANSLUCENT.and(Surface.blur(100, 100)))
                .horizontalAlignment(HorizontalAlignment.LEFT)
                .verticalAlignment(VerticalAlignment.TOP)
                .padding(Insets.of(20))
        ;
    }

    // Ensure redirection to last screen on close
    @Override
    public void onClose() {
        if (this.minecraft == null) {
            LOGGER.error("Cannot close ConfigScreenRichWindowMaximized: Minecraft is null");
            throw new IllegalStateException("Minecraft is null while closing ConfigScreenRichWindowMaximized");
        }
        this.minecraft.setScreen(parent);
    }

    // Helper method to add children
    public ConfigScreenRichWindowMaximized child(UIComponent component) {
        this.children.add(component);
        return this;
    }
}
