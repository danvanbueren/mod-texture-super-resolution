package me.danvb10.mtsr.config.components;

import io.wispforest.owo.ui.component.UIComponents;
import me.danvb10.mtsr.ClientEntrypoint;
import me.danvb10.mtsr.config.ConfigScreen;
import me.danvb10.mtsr.config.MtsrConfig;
import me.danvb10.mtsr.upscale.UpscaleManager;
import me.danvb10.mtsr.upscale.detect.TextureDetector;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import static me.danvb10.mtsr.config.components.RichWindowTypes.GENERAL_SETTINGS_WINDOW;

public class GeneralSettingsWindow extends AbstractRichWindow<GeneralSettingsWindow> {

    public GeneralSettingsWindow(ConfigScreen parent) {
        super(parent, GENERAL_SETTINGS_WINDOW);
    }

    @Override
    protected String windowName() {
        return "General Settings";
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
        MtsrConfig config = manager.config();

        window.child(UIComponents.label(
                Component.literal("Pipeline Status: ")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.literal("Enabled (Daemon Active)").withStyle(ChatFormatting.GREEN))));

        window.child(UIComponents.label(
                Component.literal("Target Textures: ")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.literal("textures/**/*.png").withStyle(ChatFormatting.WHITE))));

        window.child(UIComponents.label(
                Component.literal("Excluded Namespaces: ")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(String.join(", ",
                                TextureDetector.excludedNamespaces(config))).withStyle(ChatFormatting.WHITE))));

        window.child(UIComponents.label(
                Component.literal("Tile Size: ")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(config.tileSize() + " px").withStyle(ChatFormatting.WHITE))));

        window.child(UIComponents.label(
                Component.literal("Tile Overlap: ")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(config.tileOverlap() + " px").withStyle(ChatFormatting.WHITE))));

        window.child(UIComponents.label(
                Component.literal("Worker Threads: ")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(Integer.toString(config.workerThreads()))
                                .withStyle(ChatFormatting.WHITE))));

        window.child(UIComponents.label(
                Component.literal("Execution Provider: ")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(config.executionProvider().name())
                                .withStyle(ChatFormatting.WHITE))));

        window.child(UIComponents.label(
                Component.literal("Animated Textures: ")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(Boolean.toString(config.upscaleAnimatedTextures()))
                                .withStyle(ChatFormatting.WHITE))));
    }
}
