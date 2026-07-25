package me.danvb10.mtsr.config.components;

import io.wispforest.owo.ui.component.UIComponents;
import me.danvb10.mtsr.ClientEntrypoint;
import me.danvb10.mtsr.config.ConfigScreen;
import me.danvb10.mtsr.upscale.UpscaleManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import static me.danvb10.mtsr.config.components.RichWindowTypes.TEXTURE_MANAGER_WINDOW;

public class TextureManagerWindow extends AbstractRichWindow<TextureManagerWindow> {

    public TextureManagerWindow(ConfigScreen parent) {
        super(parent, TEXTURE_MANAGER_WINDOW);
    }

    @Override
    protected String windowName() {
        return "Texture Manager";
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

        window.child(UIComponents.label(
                Component.literal("Detection Policy:").withStyle(ChatFormatting.GRAY)));
        window.child(UIComponents.label(
                Component.literal("  - Intercepts mod block & item atlas sprites")
                        .withStyle(ChatFormatting.WHITE)));
        window.child(UIComponents.label(
                Component.literal("  - Preserves UV alignment via exact factor validation")
                        .withStyle(ChatFormatting.WHITE)));
        window.child(UIComponents.label(
                Component.literal("  - Disk cache stored at '.minecraft/mtsr/cache'")
                        .withStyle(ChatFormatting.WHITE)));

        window.child(UIComponents.label(
                Component.literal("Live Texture Metrics:").withStyle(ChatFormatting.GRAY)));

        window.child(new LiveLabelComponent(() -> {
            int hits = manager.cacheHitCount();
            int upscaled = manager.upscaledCount();
            int total = hits + upscaled;
            String percent = total == 0 ? "0%" : String.format("%.1f%%", (hits * 100.0) / total);
            return Component.literal("  Cache Hit Efficiency: ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(percent + " (" + hits + "/" + total + ")").withStyle(ChatFormatting.WHITE));
        }));

        window.child(new LiveLabelComponent(() -> {
            int failed = manager.failedCount();
            return Component.literal("  Failed Textures: ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(String.valueOf(failed))
                            .withStyle(failed > 0 ? ChatFormatting.RED : ChatFormatting.GREEN));
        }));

        window.child(new LiveLabelComponent(() -> {
            int skipped = manager.skippedCount();
            return Component.literal("  Skipped Textures: ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(String.valueOf(skipped))
                            .withStyle(ChatFormatting.YELLOW));
        }));
    }
}
