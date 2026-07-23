package me.danvb10.mtsr.config.components;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.CollapsibleContainer;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import me.danvb10.mtsr.config.ConfigScreen;
import me.danvb10.mtsr.config.ConfigScreenRichWindowMaximized;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

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
    private final ArrayList<UIComponent> children;
    private ArrayList<UIComponent> prerequisiteChildren;
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
    private ArrayList<UIComponent> buildPrerequisiteChildren() {
        UIComponent title =
                UIComponents.label(Component.literal(this.windowName))
                        .tooltip(Component.literal(this.windowTooltip))
                ;

        UIComponent minButton =
                UIComponents.button(Component.literal("-"), button -> {this.collapsibleContainer.toggleExpansion();})
                        .verticalSizing(Sizing.fixed(10))
                        .horizontalSizing(Sizing.fixed(10))
                        .margins(Insets.right(5))
                ;

        UIComponent maxButton =
                UIComponents.button(Component.literal("[]"), this::toggleMaximized)
                        .verticalSizing(Sizing.fixed(10))
                        .horizontalSizing(Sizing.fixed(10))
                ;

        FlowLayout minMaxButtons = UIContainers.horizontalFlow(Sizing.content(), Sizing.content());
        if (!minimizeIsDisabled) minMaxButtons.child(minButton);
        minMaxButtons.child(maxButton);
        minMaxButtons.horizontalAlignment(HorizontalAlignment.RIGHT);

        UIComponent parentTitle =
                UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content())
                        .child(
                                UIContainers.horizontalFlow(Sizing.fill(70), Sizing.content())
                                        .child(title)
                        )
                        .child(
                                UIContainers.horizontalFlow(Sizing.fill(30), Sizing.content())
                                        .child(minMaxButtons)
                                        .horizontalAlignment(HorizontalAlignment.RIGHT)
                        )
                        .horizontalAlignment(HorizontalAlignment.LEFT)
                        .verticalAlignment(VerticalAlignment.CENTER)
                ;

        UIComponent titleDivider =
                UIComponents.box(Sizing.fill(100), Sizing.fixed(1))
                        .color(Color.ofFormatting(ChatFormatting.DARK_GRAY))
                        .margins(Insets.vertical(5))
                ;

        ArrayList<UIComponent> c = new ArrayList<>();
        c.add(parentTitle);
        c.add(titleDivider);
        return c;
    }

    // Handle toggle maximization
    private void toggleMaximized(ButtonComponent button) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            LOGGER.error("RichWindow[" + this.uuid + "]: cannot toggle maximization, Minecraft is null");
            throw new IllegalStateException("Minecraft is null while toggling maximization for " + this.richWindowType);
        }

        if (this.maximized) {
            // Minimize
            log("Minimizing " + this.richWindowType);

            client.setScreen(this.owner);
        } else {
            // Maximize
            log("Maximizing " + this.richWindowType);

            UIComponent newComponent;

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
    public UIComponent build() {
        this.prerequisiteChildren = buildPrerequisiteChildren();

        ArrayList<UIComponent> copiedPrerequisiteChildrenArrayList = new ArrayList<>(this.prerequisiteChildren);
        FlowLayout flowLayout;

        if (fullHeight) {
            flowLayout = UIContainers.verticalFlow(Sizing.fill(100), Sizing.fill(100));

            ScrollContainer<FlowLayout> scrollableContainer = UIContainers
                    .verticalScroll(Sizing.fill(100), Sizing.fill(100),
                            UIContainers
                                    .verticalFlow(Sizing.fill(100), Sizing.content())
                                    .children(this.children)
                    );

            copiedPrerequisiteChildrenArrayList.add(scrollableContainer);
        } else {
            flowLayout = UIContainers.verticalFlow(Sizing.fill(100), Sizing.content());

            this.collapsibleContainer = (CollapsibleContainer) UIContainers
                    .collapsible(Sizing.content(), Sizing.content(), Component.literal(""), true)
                    .children(this.children);

            copiedPrerequisiteChildrenArrayList.add(this.collapsibleContainer);
        }

        ParentUIComponent rootLayout = flowLayout.children(copiedPrerequisiteChildrenArrayList);

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
    public RichWindow child(UIComponent component) {
        this.children.add(component);
        return this;
    }

}
