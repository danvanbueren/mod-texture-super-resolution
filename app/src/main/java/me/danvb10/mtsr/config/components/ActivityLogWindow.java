package me.danvb10.mtsr.config.components;

import io.wispforest.owo.ui.component.UIComponents;
import me.danvb10.mtsr.ClientEntrypoint;
import me.danvb10.mtsr.config.ConfigScreen;
import me.danvb10.mtsr.upscale.UpscaleManager;
import me.danvb10.mtsr.upscale.model.ModelManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;

import static me.danvb10.mtsr.config.components.RichWindowTypes.ACTIVITY_LOG_WINDOW;

public class ActivityLogWindow extends AbstractRichWindow<ActivityLogWindow> {

    public ActivityLogWindow(ConfigScreen parent) {
        super(parent, ACTIVITY_LOG_WINDOW);
    }

    @Override
    protected String windowName() {
        return "Activity Log";
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

        window.child(new LiveLabelComponent(() -> {
            boolean active = manager.modelProvider() instanceof ModelManager models && models.loadedModel().isPresent();
            String status = active ? "Active (Model loaded)" : "Idle (Waiting for textures/model)";
            return Component.literal("Pipeline Status: ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(status).withStyle(active ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
        }));

        window.child(new LiveLabelComponent(() -> {
            int total = manager.queuedCount();
            int done = manager.upscaledCount() + manager.cacheHitCount();
            int skipped = manager.skippedCount();
            int failed = manager.failedCount();
            return Component.literal("Processed: ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(done + "/" + total + " completed").withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(skipped > 0 ? " (" + skipped + " skipped)" : "")
                            .withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(failed > 0 ? " (" + failed + " failed)" : "").withStyle(ChatFormatting.RED));
        }));

        window.child(new LiveLabelComponent(() -> {
            List<String> entries = manager.activityLog().snapshot();
            String output = entries.isEmpty() ? "No activity yet" : String.join("\n", entries);
            return Component.literal(output).withStyle(ChatFormatting.DARK_GRAY);
        }));
    }
}
