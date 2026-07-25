package me.danvb10.mtsr.config.components;

import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.component.DiscreteSliderComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.UIContainers;
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

import java.util.Set;
import java.util.LinkedHashSet;
import java.util.function.IntConsumer;

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
        FlowLayout namespaceList = UIContainers.verticalFlow(Sizing.fill(), Sizing.content());
        Runnable[] refreshNamespaces = new Runnable[1];
        window.child(namespace);
        window.child(UIComponents.button(Component.literal("Add namespace"), button -> {
            if (controller.addExcludedNamespace(namespace.getValue())) {
                namespace.setValue("");
                namespaceResult.text(Component.literal("Saved").withStyle(ChatFormatting.GREEN));
                refreshNamespaces[0].run();
            } else {
                namespaceResult.text(Component.literal("Enter a namespace").withStyle(ChatFormatting.YELLOW));
            }
        }));
        window.child(new LiveLabelComponent(() -> Component.literal("Effective: "
                + String.join(", ", TextureDetector.excludedNamespaces(config)))
                .withStyle(ChatFormatting.WHITE)));
        window.child(namespaceList);
        window.child(namespaceResult);
        refreshNamespaces[0] = () -> {
            namespaceList.clearChildren();
            Set<String> extras = new LinkedHashSet<>(config.extraExcludedNamespaces());
            for (String excluded : extras) {
                namespaceList.child(UIComponents.button(Component.literal("Remove " + excluded),
                        button -> {
                            controller.removeExcludedNamespace(excluded);
                            namespaceResult.text(Component.literal("Saved")
                                    .withStyle(ChatFormatting.GREEN));
                            refreshNamespaces[0].run();
                        }));
            }
        };
        refreshNamespaces[0].run();

        window.child(UIComponents.label(Component.literal(
                "Tile size (restart required):").withStyle(ChatFormatting.GRAY)));
        DiscreteSliderComponent overlapSlider = discreteSlider(config.tileOverlap(), 0,
                config.tileSize() / 2, value -> {
                    controller.mutate(c -> c.tileOverlap(value));
                }, controller);
        window.child(discreteSlider(config.tileSize(), MtsrConfig.MIN_TILE_SIZE,
                MtsrConfig.MAX_TILE_SIZE, value -> {
                    controller.mutate(c -> c.tileSize(value));
                    overlapSlider.setFromDiscreteValue(config.tileOverlap());
                }, controller));

        window.child(UIComponents.label(Component.literal(
                "Tile overlap (restart required):").withStyle(ChatFormatting.GRAY)));
        window.child(overlapSlider);

        window.child(UIComponents.label(Component.literal(
                "Worker threads (restart required):").withStyle(ChatFormatting.GRAY)));
        int processors = Math.max(1, Runtime.getRuntime().availableProcessors());
        window.child(discreteSlider(config.workerThreads(), 1, processors,
                value -> controller.mutate(c -> c.workerThreads(value)), controller));

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

    private static DiscreteSliderComponent discreteSlider(
            int current, int minimum, int maximum, IntConsumer consumer,
            MtsrConfigController controller) {
        DiscreteSliderComponent slider = UIComponents.discreteSlider(Sizing.fill(), minimum, maximum)
                .setFromDiscreteValue(current)
                .snap(true);
        slider.onChanged().subscribe(value -> consumer.accept((int) Math.round(value)));
        slider.slideEnd().subscribe(controller::persist);
        return slider;
    }
}
