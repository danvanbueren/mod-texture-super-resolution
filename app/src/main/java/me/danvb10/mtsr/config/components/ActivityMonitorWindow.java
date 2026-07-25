package me.danvb10.mtsr.config.components;

import io.wispforest.owo.ui.component.UIComponents;
import me.danvb10.mtsr.ClientEntrypoint;
import me.danvb10.mtsr.config.ConfigScreen;
import me.danvb10.mtsr.upscale.UpscaleManager;
import me.danvb10.mtsr.upscale.model.ModelManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.function.Function;

import static me.danvb10.mtsr.config.components.RichWindowTypes.ACTIVITY_MONITOR_WINDOW;

public class ActivityMonitorWindow extends AbstractRichWindow<ActivityMonitorWindow> {

    public ActivityMonitorWindow(ConfigScreen parent) {
        super(parent, ACTIVITY_MONITOR_WINDOW);
    }

    @Override
    protected String windowName() {
        return "Activity Monitor";
    }

    @Override
    protected boolean dockedFullHeight() {
        return true;
    }

    @Override
    protected void populateContent(RichWindow window) {
        UpscaleManager manager = ClientEntrypoint.upscaleManager();
        if (manager == null) {
            window.child(UIComponents.label(
                    Component.literal("Upscale pipeline not initialized")
                            .withStyle(ChatFormatting.GRAY)));
            return;
        }
        window
                .child(statLabel("Queued", UpscaleManager::queuedCount))
                .child(statLabel("Upscaled", UpscaleManager::upscaledCount))
                .child(statLabel("Cache hits", UpscaleManager::cacheHitCount))
                .child(statLabel("Skipped", UpscaleManager::skippedCount))
                .child(statLabel("Failed", UpscaleManager::failedCount))
                .child(new LiveLabelComponent(() -> {
                    String provider = manager.modelProvider() instanceof ModelManager models
                            ? models.effectiveExecutionProvider().name() : "unknown";
                    return Component.literal("Provider: ")
                            .withStyle(ChatFormatting.GRAY)
                            .append(Component.literal(provider).withStyle(ChatFormatting.WHITE));
                }));
    }

    private static LiveLabelComponent statLabel(String name, Function<UpscaleManager, Integer> counter) {
        return new LiveLabelComponent(() -> {
            UpscaleManager manager = ClientEntrypoint.upscaleManager();
            String value = manager == null ? "-" : String.valueOf(counter.apply(manager));
            return Component.literal(name + ": ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(value).withStyle(ChatFormatting.WHITE));
        });
    }
}
