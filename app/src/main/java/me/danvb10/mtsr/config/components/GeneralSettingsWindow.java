package me.danvb10.mtsr.config.components;

import io.wispforest.owo.ui.component.UIComponents;
import me.danvb10.mtsr.ClientEntrypoint;
import me.danvb10.mtsr.config.ConfigScreen;
import me.danvb10.mtsr.upscale.UpscaleManager;
import me.danvb10.mtsr.upscale.detect.TextureDetector;
import me.danvb10.mtsr.upscale.model.ModelManager;
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
                        .append(Component.literal(String.join(", ", TextureDetector.EXCLUDED_NAMESPACES)).withStyle(ChatFormatting.WHITE))));

        window.child(UIComponents.label(
                Component.literal("Default Tile Size: ")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(ModelManager.DEFAULT_TILE_SIZE + " px").withStyle(ChatFormatting.WHITE))));

        window.child(UIComponents.label(
                Component.literal("Tile Overlap: ")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(ModelManager.DEFAULT_TILE_OVERLAP + " px").withStyle(ChatFormatting.WHITE))));
    }
}

