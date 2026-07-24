package me.danvb10.mtsr.config.components;

import io.wispforest.owo.ui.component.UIComponents;
import me.danvb10.mtsr.ClientEntrypoint;
import me.danvb10.mtsr.config.ConfigScreen;
import me.danvb10.mtsr.upscale.UpscaleManager;
import me.danvb10.mtsr.upscale.model.ModelManager;
import me.danvb10.mtsr.upscale.model.UpscaleModel;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static me.danvb10.mtsr.config.components.RichWindowTypes.MODEL_SETTINGS_WINDOW;

public class ModelSettingsWindow extends AbstractRichWindow<ModelSettingsWindow> {

    public ModelSettingsWindow(ConfigScreen parent) {
        super(parent, MODEL_SETTINGS_WINDOW);
    }

    @Override
    protected String windowName() {
        return "Model Settings";
    }

    @Override
    protected void populateContent(RichWindow window) {
        UpscaleManager manager = ClientEntrypoint.upscaleManager();
        if (manager == null || !(manager.modelProvider() instanceof ModelManager models)) {
            window.child(UIComponents.label(
                    Component.literal("Upscale pipeline not initialized")
                            .withStyle(ChatFormatting.GRAY)));
            return;
        }

        window.child(new LiveLabelComponent(() -> {
            Optional<UpscaleModel> active = models.loadedModel();
            String value = active
                    .map(model -> model.name() + " (" + model.scaleFactor() + "x)")
                    .orElse("none loaded");
            return Component.literal("Active model: ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(value).withStyle(ChatFormatting.WHITE));
        }));

        window.child(UIComponents.label(
                Component.literal("Available models:").withStyle(ChatFormatting.GRAY)));

        List<Path> available = models.availableModels();
        if (available.isEmpty()) {
            window.child(UIComponents.label(
                    Component.literal("  No .onnx models found in config/mtsr/models")
                            .withStyle(ChatFormatting.DARK_GRAY)));
            return;
        }
        for (Path model : available) {
            window.child(UIComponents.label(
                    Component.literal("  " + model.getFileName())
                            .withStyle(ChatFormatting.WHITE)));
        }
    }
}
