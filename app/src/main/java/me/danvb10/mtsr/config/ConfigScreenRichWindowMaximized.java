package me.danvb10.mtsr.config;

import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import me.danvb10.mtsr.config.components.*;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;

import static me.danvb10.mtsr.ClientEntrypoint.LOGGER;
import static me.danvb10.mtsr.config.ConfigScreen.getScreenTitle;

public class ConfigScreenRichWindowMaximized extends BaseOwoScreen<FlowLayout> implements RefreshableScreen {

    private final Screen parent;
    private final ConfigScreen ownerScreen;
    private final RichWindowTypes windowType;
    private volatile boolean needsRefresh;
    private Runnable batchListener;

    public ConfigScreenRichWindowMaximized(ConfigScreen ownerScreen, RichWindowTypes windowType) {
        this.parent = ownerScreen;
        this.ownerScreen = ownerScreen;
        this.windowType = windowType;
        this.batchListener = this::requestRefresh;
    }

    public ConfigScreenRichWindowMaximized(Screen parent) {
        this.parent = parent;
        this.ownerScreen = parent instanceof ConfigScreen cs ? cs : null;
        this.windowType = RichWindowTypes.DEFAULT_WINDOW;
        this.batchListener = this::requestRefresh;
    }

    @Override
    public void requestRefresh() {
        this.needsRefresh = true;
    }

    @Override
    protected void init() {
        super.init();
        me.danvb10.mtsr.upscale.UpscaleManager manager = me.danvb10.mtsr.ClientEntrypoint.upscaleManager();
        if (manager != null) {
            manager.addBatchCompletionListener(batchListener);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (needsRefresh) {
            needsRefresh = false;
            if (this.minecraft != null && this.minecraft.screen == this) {
                this.init(this.width, this.height);
            }
        }
    }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, UIContainers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout root) {
        ConfigScreen owner = ownerScreen != null ? ownerScreen : new ConfigScreen(parent);
        UIComponent windowComponent = switch (windowType) {
            case GENERAL_SETTINGS_WINDOW -> new GeneralSettingsWindow(owner).setFullscreen(true).build();
            case MODEL_SETTINGS_WINDOW -> new ModelSettingsWindow(owner).setFullscreen(true).build();
            case QUICK_ACTIONS_WINDOW -> new QuickActionsWindow(owner).setFullscreen(true).build();
            case TEXTURE_MANAGER_WINDOW -> new TextureManagerWindow(owner).setFullscreen(true).build();
            case ACTIVITY_MONITOR_WINDOW -> new ActivityMonitorWindow(owner).setFullscreen(true).build();
            case ACTIVITY_LOG_WINDOW -> new ActivityLogWindow(owner).setFullscreen(true).build();
            default -> new RichWindow(owner, windowType).setFullHeight(true).setMaximized(true).setMinimizeIsDisabled(true).build();
        };

        root
                .child(getScreenTitle(parent))
                .child(
                        UIContainers.horizontalFlow(Sizing.fill(100), Sizing.fill(94))
                                .child(
                                        UIContainers.verticalScroll(Sizing.fill(100), Sizing.fill(100),
                                                        UIContainers.verticalFlow(Sizing.fill(100), Sizing.content())
                                                                .child(
                                                                        UIContainers.verticalFlow(Sizing.fill(100), Sizing.content())
                                                                                .child(windowComponent)
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
                .padding(Insets.of(20));
    }

    @Override
    public void onClose() {
        me.danvb10.mtsr.upscale.UpscaleManager manager = me.danvb10.mtsr.ClientEntrypoint.upscaleManager();
        if (manager != null && batchListener != null) {
            manager.removeBatchCompletionListener(batchListener);
        }
        if (this.minecraft == null) {
            LOGGER.error("Cannot close ConfigScreenRichWindowMaximized: Minecraft is null");
            throw new IllegalStateException("Minecraft is null while closing ConfigScreenRichWindowMaximized");
        }
        this.minecraft.setScreen(parent);
    }

    public ConfigScreenRichWindowMaximized child(UIComponent component) {
        return this;
    }
}
