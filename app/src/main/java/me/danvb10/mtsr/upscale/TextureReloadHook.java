package me.danvb10.mtsr.upscale;

import com.mojang.blaze3d.platform.NativeImage;
import me.danvb10.mtsr.ClientEntrypoint;
import me.danvb10.mtsr.config.MtsrConfig;
import me.danvb10.mtsr.upscale.detect.TextureDetector;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.reloader.SimpleReloadListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Detects mod-provided textures during resource reload and feeds them through
 * the {@link UpscaleManager}. Upscaled results are registered back with the
 * {@code TextureManager} under the original identifier on the render thread,
 * transparently replacing the low-resolution originals.
 */
@Environment(EnvType.CLIENT)
public final class TextureReloadHook extends SimpleReloadListener<Map<Identifier, byte[]>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientEntrypoint.MOD_ID);

    private final UpscaleManager upscaleManager;
    private final MtsrConfig config;

    private TextureReloadHook(UpscaleManager upscaleManager, MtsrConfig config) {
        this.upscaleManager = upscaleManager;
        this.config = config;
    }

    /** Registers the hook for client resource reloads. */
    public static void register(UpscaleManager upscaleManager) {
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(
                ClientEntrypoint.id("texture_upscaler"),
                new TextureReloadHook(upscaleManager, ClientEntrypoint.config()));
    }

    @Override
    protected Map<Identifier, byte[]> prepare(PreparableReloadListener.SharedState state) {
        Map<Identifier, Resource> textures = state.resourceManager().listResources("textures",
                id -> TextureDetector.isModTexture(id.getNamespace(), id.getPath(), config));
        Map<Identifier, byte[]> loaded = new HashMap<>(textures.size());
        for (Map.Entry<Identifier, Resource> entry : textures.entrySet()) {
            try {
                if (entry.getValue().metadata()
                        .getSection(AnimationMetadataSection.TYPE).isPresent()) {
                    continue;
                }
            } catch (IOException e) {
                LOGGER.warn("Failed to read metadata for {}", entry.getKey(), e);
                continue;
            }
            try (InputStream stream = entry.getValue().open()) {
                loaded.put(entry.getKey(), stream.readAllBytes());
            } catch (IOException e) {
                LOGGER.warn("Failed to read texture {}", entry.getKey(), e);
            }
        }
        return loaded;
    }

    @Override
    protected void apply(Map<Identifier, byte[]> textures,
                         PreparableReloadListener.SharedState state) {
        LOGGER.info("Detected {} mod textures for upscaling", textures.size());
        upscaleManager.beginBatch();
        try {
            for (Map.Entry<Identifier, byte[]> entry : textures.entrySet()) {
                Identifier id = entry.getKey();
                upscaleManager.queueTexture(id.toString(), entry.getValue(),
                        (textureId, upscaledPng) -> registerUpscaled(id, upscaledPng));
            }
        } finally {
            upscaleManager.endBatch();
        }
    }

    private static void registerUpscaled(Identifier id, byte[] upscaledPng) {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> {
            try {
                NativeImage image = NativeImage.read(upscaledPng);
                DynamicTexture texture = new DynamicTexture(id::toString, image);
                client.getTextureManager().register(id, texture);
            } catch (IOException e) {
                LOGGER.warn("Failed to register upscaled texture {}", id, e);
            }
        });
    }
}
