package me.danvb10.mtsr.config.components;

import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import me.danvb10.mtsr.ClientEntrypoint;
import me.danvb10.mtsr.config.ConfigScreen;
import me.danvb10.mtsr.config.MtsrConfig;
import me.danvb10.mtsr.upscale.UpscaleManager;
import me.danvb10.mtsr.upscale.detect.DetectedTexture;
import me.danvb10.mtsr.upscale.detect.DetectedTextureRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Map;

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

        DetectedTextureRegistry registry = manager.detectedTextureRegistry();
        MtsrConfig config = ClientEntrypoint.config();

        // 1. Summary Bar & Global Action Toolbar
        FlowLayout summaryBar = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content());
        summaryBar.padding(Insets.of(6));
        summaryBar.margins(Insets.bottom(8));
        summaryBar.surface(Surface.flat(0xFF141414));
        summaryBar.verticalAlignment(VerticalAlignment.CENTER);

        summaryBar.child(new LiveLabelComponent(() -> {
            int total = registry.totalCount();
            int upscaled = registry.upscaledCount();
            int disabled = registry.disabledCount();
            int tagged = registry.taggedCount();
            return Component.literal("Textures: ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(String.valueOf(total)).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(" | Upscaled: ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(String.valueOf(upscaled)).withStyle(ChatFormatting.GREEN))
                    .append(Component.literal(" | Disabled: ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(String.valueOf(disabled)).withStyle(ChatFormatting.RED))
                    .append(Component.literal(" | Tagged Bad: ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(String.valueOf(tagged)).withStyle(tagged > 0 ? ChatFormatting.GOLD : ChatFormatting.DARK_GRAY));
        }));

        summaryBar.child(
                UIComponents.button(Component.literal("Regenerate All Tagged"), button -> {
                    manager.regenerateTagged();
                    saveConfig(config);
                    requestScreenRefresh();
                }).horizontalSizing(Sizing.content(6)).verticalSizing(Sizing.fixed(16))
        );

        window.child(summaryBar);

        Map<String, List<DetectedTexture>> allTextures = registry.getAllTextures();

        if (allTextures.isEmpty()) {
            FlowLayout emptyLayout = UIContainers.verticalFlow(Sizing.fill(100), Sizing.fixed(80));
            emptyLayout.child(UIComponents.label(Component.literal("No mod textures detected yet.")
                    .withStyle(ChatFormatting.GRAY)));
            emptyLayout.child(UIComponents.label(Component.literal("Load a world or resource pack to capture textures.")
                    .withStyle(ChatFormatting.DARK_GRAY)));
            emptyLayout.horizontalAlignment(HorizontalAlignment.CENTER);
            emptyLayout.verticalAlignment(VerticalAlignment.CENTER);
            window.child(emptyLayout);
            return;
        }

        // 2. Mod / Namespace Groups
        for (Map.Entry<String, List<DetectedTexture>> entry : allTextures.entrySet()) {
            String namespace = entry.getKey();
            List<DetectedTexture> textures = entry.getValue();

            boolean groupDisabled = config.disabledNamespaces().contains(namespace);

            // Group Header Container
            FlowLayout groupHeader = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content());
            groupHeader.padding(Insets.of(4));
            groupHeader.verticalAlignment(VerticalAlignment.CENTER);

            groupHeader.child(UIComponents.label(
                    Component.literal("Mod: " + namespace)
                            .withStyle(groupDisabled ? ChatFormatting.STRIKETHROUGH : ChatFormatting.BOLD)
                            .withStyle(groupDisabled ? ChatFormatting.GRAY : ChatFormatting.GOLD)
                            .append(Component.literal(" (" + textures.size() + " textures)").withStyle(ChatFormatting.DARK_GRAY))
            ));

            FlowLayout groupActions = UIContainers.horizontalFlow(Sizing.content(), Sizing.content());
            groupActions.horizontalAlignment(HorizontalAlignment.RIGHT);

            groupActions.child(UIComponents.button(
                    Component.literal(groupDisabled ? "Enable Group" : "Disable Group"),
                    button -> {
                        boolean newDisabledState = !config.disabledNamespaces().contains(namespace);
                        config.setNamespaceEnabled(namespace, !newDisabledState);
                        registry.setNamespaceDisabled(namespace, newDisabledState);
                        saveConfig(config);
                        requestScreenRefresh();
                    }
            ).horizontalSizing(Sizing.content(4)).verticalSizing(Sizing.fixed(14)).margins(Insets.right(4)));

            groupActions.child(UIComponents.button(
                    Component.literal("Regenerate Group"),
                    button -> {
                        manager.regenerateNamespace(namespace);
                        saveConfig(config);
                        requestScreenRefresh();
                    }
            ).horizontalSizing(Sizing.content(4)).verticalSizing(Sizing.fixed(14)));

            FlowLayout groupBar = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content());
            groupBar.child(groupHeader);
            groupBar.child(groupActions);
            groupBar.horizontalAlignment(HorizontalAlignment.LEFT);
            groupBar.verticalAlignment(VerticalAlignment.CENTER);

            // Horizontal Scroll Container for texture columns
            FlowLayout textureGridContainer = UIContainers.horizontalFlow(Sizing.content(), Sizing.content());
            textureGridContainer.padding(Insets.of(4));

            for (DetectedTexture texture : textures) {
                textureGridContainer.child(buildTextureColumnCard(texture, manager, config));
            }

            var scrollableGrid = UIContainers.horizontalScroll(
                    Sizing.fill(100), Sizing.content(), textureGridContainer
            ).scrollbarThiccness(4);

            var collapsibleGroup = UIContainers.collapsible(
                    Sizing.fill(100), Sizing.content(),
                    Component.literal("Namespace: " + namespace), false
            );
            collapsibleGroup.child(groupBar);
            collapsibleGroup.child(scrollableGrid);
            collapsibleGroup.padding(Insets.of(4));
            collapsibleGroup.margins(Insets.bottom(6));

            window.child(collapsibleGroup);
        }
    }

    private FlowLayout buildTextureColumnCard(DetectedTexture texture, UpscaleManager manager, MtsrConfig config) {
        FlowLayout card = UIContainers.verticalFlow(Sizing.fixed(120), Sizing.content());
        card.padding(Insets.of(6));
        card.margins(Insets.right(6));
        card.surface(Surface.flat(texture.isDisabled() ? 0xFF181818 : 0xFF222222));
        card.horizontalAlignment(HorizontalAlignment.CENTER);

        // Texture Name Header
        String name = texture.path();
        if (name.contains("/")) {
            name = name.substring(name.lastIndexOf('/') + 1);
        }
        card.child(UIComponents.label(Component.literal(name).withStyle(ChatFormatting.GRAY))
                .tooltip(Component.literal(texture.textureId())));

        card.child(UIComponents.box(Sizing.fill(100), Sizing.fixed(1))
                .color(Color.ofFormatting(ChatFormatting.DARK_GRAY))
                .margins(Insets.vertical(4)));

        // Row 1: Original Texture Preview
        card.child(UIComponents.label(Component.literal("Original").withStyle(ChatFormatting.DARK_GRAY)));
        card.child(DynamicTexturePreview.createPreview(texture.textureId() + "_orig", texture.originalPng(), 48, 48));
        String origDimStr = texture.originalWidth() > 0 ? texture.originalWidth() + "x" + texture.originalHeight() : "-";
        card.child(UIComponents.label(Component.literal(origDimStr).withStyle(ChatFormatting.DARK_GRAY)).margins(Insets.bottom(4)));

        // Row 2: Generated / Upscaled Texture Preview
        card.child(UIComponents.label(Component.literal("Generated").withStyle(ChatFormatting.DARK_GRAY)));
        card.child(DynamicTexturePreview.createPreview(texture.textureId() + "_up", texture.upscaledPng(), 48, 48));
        String upDimStr = texture.upscaledWidth() > 0 ? texture.upscaledWidth() + "x" + texture.upscaledHeight() : "-";
        card.child(UIComponents.label(Component.literal(upDimStr).withStyle(ChatFormatting.DARK_GRAY)).margins(Insets.bottom(4)));

        // Row 3: Status Badge
        ChatFormatting statusFormat = switch (texture.status()) {
            case UPSCALED, CACHE_HIT -> ChatFormatting.GREEN;
            case DISABLED -> ChatFormatting.RED;
            case FAILED -> ChatFormatting.DARK_RED;
            case QUEUED -> ChatFormatting.YELLOW;
            default -> ChatFormatting.GRAY;
        };
        card.child(UIComponents.label(Component.literal(texture.status().displayName())
                .withStyle(statusFormat))
                .margins(Insets.bottom(4)));

        // Row 4: Controls
        // Enable/Disable Toggle
        boolean isDisabled = texture.isDisabled() || config.disabledTextureIds().contains(texture.textureId());
        card.child(UIComponents.button(
                Component.literal(isDisabled ? "Enable" : "Disable"),
                button -> {
                    boolean newEnable = isDisabled;
                    config.setTextureEnabled(texture.textureId(), newEnable);
                    texture.setDisabled(!newEnable);
                    if (!newEnable) {
                        texture.status(DetectedTexture.Status.DISABLED);
                    }
                    saveConfig(config);
                    requestScreenRefresh();
                }
        ).horizontalSizing(Sizing.fixed(100)).verticalSizing(Sizing.fixed(14)).margins(Insets.bottom(2)));

        // Regenerate Button
        card.child(UIComponents.button(
                Component.literal("Regenerate"),
                button -> {
                    manager.regenerateTexture(texture.textureId());
                    saveConfig(config);
                    requestScreenRefresh();
                }
        ).horizontalSizing(Sizing.fixed(100)).verticalSizing(Sizing.fixed(14)).margins(Insets.bottom(2)));

        // Tag Bad Upscale Button
        boolean isTagged = texture.isTaggedForRegen();
        card.child(UIComponents.button(
                Component.literal(isTagged ? "Tagged (Bad)" : "Tag Bad Upscale")
                        .withStyle(isTagged ? ChatFormatting.GOLD : ChatFormatting.GRAY),
                button -> {
                    boolean newTagged = !texture.isTaggedForRegen();
                    texture.setTaggedForRegen(newTagged);
                    config.setTextureTaggedForRegen(texture.textureId(), newTagged);
                    saveConfig(config);
                    requestScreenRefresh();
                }
        ).horizontalSizing(Sizing.fixed(100)).verticalSizing(Sizing.fixed(14)));

        return card;
    }

    private static void saveConfig(MtsrConfig config) {
        if (ClientEntrypoint.configStore() != null) {
            try {
                ClientEntrypoint.configStore().save(config);
            } catch (Exception ignored) {
            }
        }
    }
}
