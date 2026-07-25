package me.danvb10.mtsr.config.components;

import com.mojang.blaze3d.platform.NativeImage;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility to convert raw PNG byte arrays into dynamic Minecraft GUI texture components.
 */
public final class DynamicTexturePreview {

    private static final Logger LOGGER = LoggerFactory.getLogger("mtsr");
    private static final Map<String, Identifier> REGISTERED_TEXTURES = new ConcurrentHashMap<>();

    private DynamicTexturePreview() {
    }

    /**
     * Creates a UIComponent rendering the PNG byte array at fixed preview size (e.g. 48x48).
     */
    public static UIComponent createPreview(String textureKey, byte[] pngBytes, int previewWidth, int previewHeight) {
        if (pngBytes == null || pngBytes.length == 0) {
            return createPlaceholder(previewWidth, previewHeight, "Pending", Color.ofFormatting(ChatFormatting.DARK_GRAY));
        }

        try {
            NativeImage image = NativeImage.read(new ByteArrayInputStream(pngBytes));
            int imgW = image.getWidth();
            int imgH = image.getHeight();

            Identifier id = REGISTERED_TEXTURES.computeIfAbsent(textureKey + "_" + imgW + "x" + imgH + "_" + pngBytes.length, k -> {
                Identifier identifier = Identifier.fromNamespaceAndPath("mtsr", "dynamic_preview/" + UUID.randomUUID());
                DynamicTexture texture = new DynamicTexture(() -> "mtsr_preview", image);
                Minecraft.getInstance().getTextureManager().register(identifier, texture);
                return identifier;
            });

            return UIContainers.horizontalFlow(Sizing.fixed(previewWidth), Sizing.fixed(previewHeight))
                    .child(UIComponents.texture(id, 0, 0, imgW, imgH, imgW, imgH)
                            .sizing(Sizing.fixed(previewWidth), Sizing.fixed(previewHeight)))
                    .horizontalAlignment(HorizontalAlignment.CENTER)
                    .verticalAlignment(VerticalAlignment.CENTER)
                    .surface(Surface.flat(0xFF101010))
                    .padding(Insets.of(2));
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Failed to create preview for {}", textureKey, e);
            return createPlaceholder(previewWidth, previewHeight, "Error", Color.ofFormatting(ChatFormatting.RED));
        }
    }

    public static UIComponent createPlaceholder(int width, int height, String text, Color bgColor) {
        return UIContainers.horizontalFlow(Sizing.fixed(width), Sizing.fixed(height))
                .child(UIComponents.label(Component.literal(text).withStyle(ChatFormatting.DARK_GRAY)))
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER)
                .surface(Surface.flat(0xFF1E1E1E))
                .padding(Insets.of(2));
    }
}
