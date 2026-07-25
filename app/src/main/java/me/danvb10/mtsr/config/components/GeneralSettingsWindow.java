package me.danvb10.mtsr.config.components;

import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.core.Sizing;
import me.danvb10.mtsr.ClientEntrypoint;
import me.danvb10.mtsr.config.ConfigScreen;
import me.danvb10.mtsr.config.MtsrConfig;
import me.danvb10.mtsr.config.MtsrConfigController;
import me.danvb10.mtsr.config.ExecutionProvider;
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
        MtsrConfigController controller = new MtsrConfigController(
                config, ClientEntrypoint.configStore());

        window.child(UIComponents.label(
                Component.literal("Pipeline Status: ")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.literal("Enabled (Daemon Active)").withStyle(ChatFormatting.GREEN))));

        window.child(UIComponents.label(
                Component.literal("Target Textures: ")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.literal("textures/**/*.png").withStyle(ChatFormatting.WHITE))));

        window.child(UIComponents.label(Component.literal("Excluded Namespaces:")
                .withStyle(ChatFormatting.GRAY)));
        TextBoxComponent namespace = UIComponents.textBox(Sizing.fixed(140));
        LabelComponent namespaceResult = UIComponents.label(Component.empty());
        window.child(namespace);
        window.child(UIComponents.button(Component.literal("Add namespace"), button -> {
            if (controller.addExcludedNamespace(namespace.getValue())) {
                namespace.setValue("");
                namespaceResult.text(Component.literal("Saved").withStyle(ChatFormatting.GREEN));
            } else {
                namespaceResult.text(Component.literal("Enter a namespace").withStyle(ChatFormatting.YELLOW));
            }
        }));
        for (String excluded : config.extraExcludedNamespaces()) {
            window.child(UIComponents.button(Component.literal("Remove " + excluded), button -> {
                controller.removeExcludedNamespace(excluded);
                namespaceResult.text(Component.literal("Saved").withStyle(ChatFormatting.GREEN));
            }));
        }
        window.child(namespaceResult);

        window.child(UIComponents.label(Component.literal(
                "Tile size (restart required):").withStyle(ChatFormatting.GRAY)));
        window.child(discreteSlider(config.tileSize(), MtsrConfig.MIN_TILE_SIZE,
                MtsrConfig.MAX_TILE_SIZE, value -> controller.update(c -> c.tileSize(value))));

        window.child(UIComponents.label(Component.literal(
                "Tile overlap (restart required):").withStyle(ChatFormatting.GRAY)));
        window.child(discreteSlider(config.tileOverlap(), 0, MtsrConfig.MAX_TILE_SIZE / 2,
                value -> controller.update(c -> c.tileOverlap(value))));

        window.child(UIComponents.label(Component.literal(
                "Worker threads (restart required):").withStyle(ChatFormatting.GRAY)));
        int processors = Math.max(1, Runtime.getRuntime().availableProcessors());
        window.child(discreteSlider(config.workerThreads(), 1, processors,
                value -> controller.update(c -> c.workerThreads(value))));

        window.child(UIComponents.label(Component.literal(
                "Execution provider (restart required):").withStyle(ChatFormatting.GRAY)));
        var providerDropdown = UIComponents.dropdown(Sizing.fixed(160))
                .text(Component.literal(config.executionProvider().name()));
        for (ExecutionProvider provider : ExecutionProvider.values()) {
            providerDropdown.button(Component.literal(provider.name()), dropdown -> {
                controller.update(c -> c.executionProvider(provider));
                dropdown.text(Component.literal(provider.name()));
            });
        }
        window.child(providerDropdown);

        window.child(UIComponents.checkbox(Component.literal("Upscale animated textures"))
                .checked(config.upscaleAnimatedTextures())
                .onChanged(value -> controller.update(c -> c.upscaleAnimatedTextures(value))));
        window.child(UIComponents.checkbox(Component.literal("Show completion toast"))
                .checked(config.showCompletionToast())
                .onChanged(value -> controller.update(c -> c.showCompletionToast(value))));
        window.child(UIComponents.label(Component.literal(
                "Tile size, worker count, and execution provider apply after restart.")
                .withStyle(ChatFormatting.YELLOW)));
    }

    private static io.wispforest.owo.ui.component.DiscreteSliderComponent discreteSlider(
            int current, int minimum, int maximum, java.util.function.IntConsumer consumer) {
        var slider = UIComponents.discreteSlider(Sizing.fill(), minimum, maximum)
                .setFromDiscreteValue(current)
                .snap(true);
        slider.onChanged().subscribe(value -> consumer.accept((int) Math.round(value)));
        return slider;
    }
}
