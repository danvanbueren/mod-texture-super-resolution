package me.danvb10.mtsr.config;

import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import net.minecraft.client.gui.screen.Screen;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

import static me.danvb10.mtsr.ClientEntrypoint.LOGGER;
import static me.danvb10.mtsr.config.ConfigScreen.getScreenTitle;

public class ConfigScreenRichWindowMaximized extends BaseOwoScreen<FlowLayout> {

    private final ArrayList<Component> children;
    private final Screen parent;

    // Constructor
    public ConfigScreenRichWindowMaximized(Screen parent) {
        this.parent = parent;
        this.children = new ArrayList<>();
    }

    // Create adapter
    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    // Build full component
    @Override
    protected void build(FlowLayout root) {
        root
                .child(getScreenTitle(parent))
                .child(
                        Containers.horizontalFlow(Sizing.fill(100), Sizing.fill(94))
                                .child(
                                        Containers.verticalScroll(Sizing.fill(100), Sizing.fill(100),
                                                        Containers.verticalFlow(Sizing.fill(100), Sizing.content())
                                                                .child(
                                                                        Containers.verticalFlow(Sizing.fill(100), Sizing.content())
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
    public void close() {
        if (this.client == null) {
            LOGGER.error("Cannot close ConfigScreenRichWindowMaximized: MinecraftClient is null");
            throw new IllegalStateException("MinecraftClient is null while closing ConfigScreenRichWindowMaximized");
        }
        this.client.setScreen(parent);
    }

    // Helper method to add children
    public ConfigScreenRichWindowMaximized child(Component component) {
        this.children.add(component);
        return this;
    }
}
