package me.danvb10.mtsr.config.components;

import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.core.Insets;
import me.danvb10.mtsr.ClientEntrypoint;
import me.danvb10.mtsr.config.ConfigScreen;
import me.danvb10.mtsr.upscale.UpscaleManager;
import me.danvb10.mtsr.upscale.model.ModelManager;
import me.danvb10.mtsr.upscale.model.UpscaleModel;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

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
        window.child(new LiveLabelComponent(() -> Component.literal("Effective provider: ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(models.effectiveExecutionProvider().name())
                        .withStyle(ChatFormatting.WHITE))));

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

        LabelComponent result = UIComponents.label(Component.empty());
        window.child(UIComponents.button(Component.literal("Switch model and reload"), button -> {
            String current = manager.config().activeModelFileName();
            if (current == null) {
                current = models.loadedModel().map(model -> model.name() + ".onnx").orElse(null);
            }
            String next = nextModel(available, current);
            if (next == null) {
                result.text(Component.literal("No alternate model available")
                        .withStyle(ChatFormatting.GRAY));
                return;
            }
            result.text(Component.literal("Loading " + next + "...")
                    .withStyle(ChatFormatting.YELLOW));
            CompletableFuture.runAsync(() -> {
                boolean selected = models.selectModel(next);
                Minecraft client = Minecraft.getInstance();
                if (client != null) {
                    client.execute(() -> {
                        if (selected) {
                            manager.cache().clear();
                            client.reloadResourcePacks();
                            result.text(Component.literal("Selected " + next + "; reloading textures")
                                    .withStyle(ChatFormatting.GREEN));
                        } else {
                            result.text(Component.literal("Failed to load " + next)
                                    .withStyle(ChatFormatting.RED));
                        }
                    });
                }
            });
        }).margins(Insets.bottom(4)));
        window.child(result);
    }

    private static String nextModel(List<Path> available, String current) {
        if (available.isEmpty()) {
            return null;
        }
        for (int i = 0; i < available.size(); i++) {
            if (available.get(i).getFileName().toString().equals(current)) {
                return available.get((i + 1) % available.size()).getFileName().toString();
            }
        }
        return available.getFirst().getFileName().toString();
    }
}
