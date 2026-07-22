package me.danvb10.mtsr.config.components;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.CollapsibleContainer;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import me.danvb10.mtsr.config.ConfigScreen;
import me.danvb10.mtsr.config.ConfigScreenRichWindowMaximized;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.UUID;

import static me.danvb10.mtsr.ClientEntrypoint.LOGGER;
import static me.danvb10.mtsr.config.components.RichWindowTypes.*;

public class RichWindow {

    private String windowName, windowTooltip;
    private final UUID uuid;
    private final ConfigScreen owner;
    private final RichWindowTypes richWindowType;
    private boolean fullHeight, maximized, minimizeIsDisabled;
    private final ArrayList<Component> children;
    private ArrayList<Component> prerequisiteChildren;
    private CollapsibleContainer collapsibleContainer;

    // Constructor
    public RichWindow(ConfigScreen owner, RichWindowTypes richWindowType) {
        this.owner = owner;
        this.richWindowType = richWindowType;
        this.windowName = "Default";
        this.windowTooltip = "Default";
        this.uuid = UUID.randomUUID();

        this.fullHeight = false;
        this.maximized = false;
        this.minimizeIsDisabled = false;

        this.children = new ArrayList<>();
        this.prerequisiteChildren = null;
    }

    // Helper method for console logging
    private void log(String message) {
        LOGGER.info("RichWindow[" + this.uuid.toString() + "]: " + message);
    }

    // Build title bar component prefix
    private ArrayList<Component> buildPrerequisiteChildren() {
        Component title =
                Components.label(Text.literal(this.windowName))
                        .tooltip(Text.literal(this.windowTooltip))
                ;

        Component minButton =
                Components.button(Text.literal("-"), button -> {this.collapsibleContainer.toggleExpansion();})
                        .verticalSizing(Sizing.fixed(10))
                        .horizontalSizing(Sizing.fixed(10))
                        .margins(Insets.right(5))
                ;

        Component maxButton =
                Components.button(Text.literal("[]"), this::toggleMaximized)
                        .verticalSizing(Sizing.fixed(10))
                        .horizontalSizing(Sizing.fixed(10))
                ;

        FlowLayout minMaxButtons = Containers.horizontalFlow(Sizing.content(), Sizing.content());
        if (!minimizeIsDisabled) minMaxButtons.child(minButton);
        minMaxButtons.child(maxButton);
        minMaxButtons.horizontalAlignment(HorizontalAlignment.RIGHT);

        Component parentTitle =
                Containers.horizontalFlow(Sizing.fill(100), Sizing.content())
                        .child(
                                Containers.horizontalFlow(Sizing.fill(70), Sizing.content())
                                        .child(title)
                        )
                        .child(
                                Containers.horizontalFlow(Sizing.fill(30), Sizing.content())
                                        .child(minMaxButtons)
                                        .horizontalAlignment(HorizontalAlignment.RIGHT)
                        )
                        .horizontalAlignment(HorizontalAlignment.LEFT)
                        .verticalAlignment(VerticalAlignment.CENTER)
                ;

        Component titleDivider =
                Components.box(Sizing.fill(100), Sizing.fixed(1))
                        .color(Color.ofFormatting(Formatting.DARK_GRAY))
                        .margins(Insets.vertical(5))
                ;

        ArrayList<Component> c = new ArrayList<>();
        c.add(parentTitle);
        c.add(titleDivider);
        return c;
    }

    // Handle toggle maximization
    private void toggleMaximized(ButtonComponent button) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            LOGGER.error("RichWindow[" + this.uuid + "]: cannot toggle maximization, MinecraftClient is null");
            throw new IllegalStateException("MinecraftClient is null while toggling maximization for " + this.richWindowType);
        }

        if (this.maximized) {
            // Minimize
            log("Minimizing " + this.richWindowType);

            client.setScreen(this.owner);
        } else {
            // Maximize
            log("Maximizing " + this.richWindowType);

            Component newComponent;

            switch (this.richWindowType) {
                case GENERAL_SETTINGS_WINDOW:
                    newComponent = new GeneralSettingsWindow(this.owner)
                            .setFullscreen(true)
                            .build();
                    break;
                case MODEL_SETTINGS_WINDOW:
                    newComponent = new ModelSettingsWindow(this.owner)
                            .setFullscreen(true)
                            .build();
                    break;
                case QUICK_ACTIONS_WINDOW:
                    newComponent = new QuickActionsWindow(this.owner)
                            .setFullscreen(true)
                            .build();
                    break;
                case TEXTURE_MANAGER_WINDOW:
                    newComponent = new TextureManagerWindow(this.owner)
                            .setFullscreen(true)
                            .build();
                    break;
                case ACTIVITY_MONITOR_WINDOW:
                    newComponent = new ActivityMonitorWindow(this.owner)
                            .setFullscreen(true)
                            .build();
                    break;
                case ACTIVITY_LOG_WINDOW:
                    newComponent = new ActivityLogWindow(this.owner)
                            .setFullscreen(true)
                            .build();
                    break;
                default:
                    newComponent = new RichWindow(this.owner, DEFAULT_WINDOW)
                            .setFullHeight(true)
                            .setMaximized(true)
                            .setMinimizeIsDisabled(true)
                            .build();
                    break;
            }

            Screen maxedScreen = new ConfigScreenRichWindowMaximized(this.owner)
                    .child(newComponent);

            client.setScreen(maxedScreen);
        }
    }

    // Build and return full component
    public Component build() {
        this.prerequisiteChildren = buildPrerequisiteChildren();

        ArrayList<Component> copiedPrerequisiteChildrenArrayList = new ArrayList<>(this.prerequisiteChildren);
        FlowLayout flowLayout;

        if (fullHeight) {
            flowLayout = Containers.verticalFlow(Sizing.fill(100), Sizing.fill(100));

            ScrollContainer<FlowLayout> scrollableContainer = Containers
                    .verticalScroll(Sizing.fill(100), Sizing.fill(100),
                            Containers
                                    .verticalFlow(Sizing.fill(100), Sizing.content())
                                    .children(this.children)
                    );

            copiedPrerequisiteChildrenArrayList.add(scrollableContainer);
        } else {
            flowLayout = Containers.verticalFlow(Sizing.fill(100), Sizing.content());

            this.collapsibleContainer = (CollapsibleContainer) Containers
                    .collapsible(Sizing.content(), Sizing.content(), Text.literal(""), true)
                    .children(this.children);

            copiedPrerequisiteChildrenArrayList.add(this.collapsibleContainer);
        }

        ParentComponent rootLayout = flowLayout.children(copiedPrerequisiteChildrenArrayList);

        rootLayout
                .padding(Insets.of(8))
                .surface(Surface.DARK_PANEL);

        return rootLayout.margins(Insets.of(0, 4, 0, 0));
    }

    // Getters & Setters
    public boolean isFullHeight() {
        return this.fullHeight;
    }
    public RichWindow setFullHeight(boolean fullHeight) {
        this.fullHeight = fullHeight;
        return this;
    }
    public boolean isMaximized() {
        return this.maximized;
    }
    public RichWindow setMaximized(boolean maximized) {
        this.maximized = maximized;
        return this;
    }
    public boolean isMinimizeIsDisabled() {
        return this.minimizeIsDisabled;
    }
    public RichWindow setMinimizeIsDisabled(boolean minimizeIsDisabled) {
        this.minimizeIsDisabled = minimizeIsDisabled;
        return this;
    }
    public String getWindowName() {
        return this.windowName;
    }
    public RichWindow setWindowName(String windowName) {
        this.windowName = windowName;
        return this;
    }
    public String getWindowTooltip() {
        return windowTooltip;
    }
    public RichWindow setWindowTooltip(String windowTooltip) {
        this.windowTooltip = windowTooltip;
        return this;
    }

    // Helper method to add children
    public RichWindow child(Component component) {
        this.children.add(component);
        return this;
    }

}
