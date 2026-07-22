package me.danvb10.mtsr.config;

import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.*;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import me.danvb10.mtsr.config.components.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import org.jetbrains.annotations.NotNull;

import static me.danvb10.mtsr.ClientEntrypoint.LOGGER;
import static me.danvb10.mtsr.config.components.RichWindowTypes.*;

public class ConfigScreen extends BaseOwoScreen<FlowLayout> {

    private final Screen parent;

    private GeneralSettingsWindow generalSettingsWindow;
    private ModelSettingsWindow modelSettingsWindow;
    private QuickActionsWindow quickActionsWindow;
    private TextureManagerWindow textureManagerWindow;
    private ActivityMonitorWindow activityMonitorWindow;
    private ActivityLogWindow activityLogWindow;

    // Constructor
    public ConfigScreen(Screen parent) {
        super();
        this.parent = parent;

        this.generalSettingsWindow = new GeneralSettingsWindow(this);
        this.modelSettingsWindow = new ModelSettingsWindow(this);
        this.quickActionsWindow = new QuickActionsWindow(this);
        this.textureManagerWindow = new TextureManagerWindow(this);
        this.activityMonitorWindow = new ActivityMonitorWindow(this);
        this.activityLogWindow = new ActivityLogWindow(this);

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
                        UIContainers.horizontalFlow(Sizing.fill(100), Sizing.fill(66))
                                .child(
                                        UIContainers.verticalScroll(Sizing.fill(33), Sizing.fill(100),
                                                UIContainers.verticalFlow(Sizing.fill(100), Sizing.content())
                                                        .child(
                                                                UIContainers.verticalFlow(Sizing.fill(100), Sizing.content())
                                                                        .child(this.generalSettingsWindow.build())
                                                                        .child(this.modelSettingsWindow.build())
                                                                        .child(this.quickActionsWindow.build())
                                                                        .padding(Insets.of(0, 0, 0, 8))
                                                        )
                                                )
                                                .scrollbarThiccness(6)
                                                .padding(Insets.of(0, 12, 0, 8))
                                )
                                .child(
                                        UIContainers.verticalScroll(Sizing.fill(66), Sizing.fill(100),
                                                        UIContainers.verticalFlow(Sizing.fill(100), Sizing.content())
                                                                .child(
                                                                        UIContainers.verticalFlow(Sizing.fill(100), Sizing.content())
                                                                                .child(this.textureManagerWindow.build())
                                                                                .padding(Insets.of(0, 0, 0, 8))
                                                                )
                                                )
                                                .scrollbarThiccness(6)
                                                .padding(Insets.of(0, 12, 0, 8))
                                )

                )
                .child(
                        UIContainers.horizontalFlow(Sizing.fill(100), Sizing.fill(28))
                                .child(
                                        UIContainers.verticalFlow(Sizing.fill(33), Sizing.fill(100))
                                                .child(
                                                        UIContainers.verticalFlow(Sizing.fill(100), Sizing.content())
                                                                .child(this.activityMonitorWindow.build())
                                                                .padding(Insets.of(0, 0, 0, 8))
                                                )
                                                .padding(Insets.of(0, 0, 0, 8))
                                )
                                .child(
                                        UIContainers.verticalFlow(Sizing.fill(66), Sizing.fill(100))
                                                .child(
                                                        UIContainers.verticalFlow(Sizing.fill(100), Sizing.content())
                                                                .child(this.activityLogWindow.build())
                                                                .padding(Insets.of(0, 0, 0, 8))
                                                )
                                                .padding(Insets.of(0, 0, 0, 8))
                                )

                )
                .surface(Surface.VANILLA_TRANSLUCENT.and(Surface.blur(100, 100)))
                .horizontalAlignment(HorizontalAlignment.LEFT)
                .verticalAlignment(VerticalAlignment.TOP)
                .padding(Insets.of(20))
        ;
    }

    // Helper method for console logging
    private void log(String message) {
        LOGGER.info("ConfigScreen: " + message);
    }

    // Create screen title component
    public static FlowLayout getScreenTitle(Screen parent) {
        return UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content())
                .child(
                        UIContainers.horizontalFlow(Sizing.fill(25), Sizing.fill(6))
                                .child(
                                        UIComponents.button(
                                                        Component.literal("Back"),
                                                        buttonComponent -> {
                                                            Minecraft.getInstance().setScreen(parent);
                                                        }
                                                )
                                                .horizontalSizing(Sizing.content(8))
                                )
                                .horizontalAlignment(HorizontalAlignment.LEFT)
                                .verticalAlignment(VerticalAlignment.TOP)
                )
                .child(
                        UIContainers.horizontalFlow(Sizing.fill(50), Sizing.fill(6))
                                .child(
                                        UIComponents.label(
                                                Component.literal("Mod Texture Super Resolution Configuration")
                                                        .withStyle(ChatFormatting.BOLD)
                                        )
                                )
                                .horizontalAlignment(HorizontalAlignment.CENTER)
                                .verticalAlignment(VerticalAlignment.TOP)
                );
    }

    // Getters & Setters
    public Screen getParent() {
        return parent;
    }

    // Ensure redirection to last screen on close
    @Override
    public void onClose() {
        if (this.minecraft == null) {
            LOGGER.error("Cannot close ConfigScreen: Minecraft is null");
            throw new IllegalStateException("Minecraft is null while closing ConfigScreen");
        }
        this.minecraft.setScreen(parent);
    }

    // Called by ModMenuIntegration
    public static Screen create(Screen parent) {
        return new ConfigScreen(parent);
    }
}
