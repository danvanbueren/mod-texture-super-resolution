package me.danvb10.mtsr.upscale;

import com.mojang.blaze3d.platform.NativeImage;
import me.danvb10.mtsr.ClientEntrypoint;
import me.danvb10.mtsr.upscale.cache.CacheKey;
import me.danvb10.mtsr.upscale.detect.DetectedTexture;
import me.danvb10.mtsr.upscale.detect.SpriteUpscalePolicy;
import me.danvb10.mtsr.upscale.model.UpscaleModel;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Wraps the vanilla {@link SpriteResourceLoader} so mod-provided atlas sprites
 * (blocks, items, ...) are swapped for their upscaled versions before the
 * atlas is stitched. Stitching is synchronous, so only cached upscale results
 * are applied immediately; cache misses are queued on the background worker
 * and picked up on the next resource reload. Minecraft's stitcher supports
 * mixed-resolution sprites, so the higher-resolution image stitches like a
 * high-res resource pack sprite would.
 */
@Environment(EnvType.CLIENT)
public final class AtlasSpriteUpscaler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientEntrypoint.MOD_ID);

    private AtlasSpriteUpscaler() {
    }

    /** Wraps the given loader, upscaling eligible mod sprites from cache. */
    public static SpriteResourceLoader wrap(SpriteResourceLoader delegate,
                                            Set<MetadataSectionType<?>> additionalMetadataSections) {
        return (spriteLocation, resource) -> {
            SpriteContents original = delegate.loadSprite(spriteLocation, resource);
            if (original == null) {
                return null;
            }
            try {
                SpriteContents upscaled =
                        tryUpscale(spriteLocation, resource, original, additionalMetadataSections);
                return upscaled != null ? upscaled : original;
            } catch (RuntimeException e) {
                LOGGER.warn("Sprite upscale failed for {}, using original", spriteLocation, e);
                return original;
            }
        };
    }

    private static SpriteContents tryUpscale(Identifier spriteLocation, Resource resource,
                                             SpriteContents original,
                                             Set<MetadataSectionType<?>> additionalMetadataSections) {
        if (!SpriteUpscalePolicy.isEligibleSprite(
                spriteLocation.getNamespace(), spriteLocation.getPath(),
                ClientEntrypoint.config())) {
            return null;
        }
        UpscaleManager manager = ClientEntrypoint.upscaleManager();
        if (manager == null) {
            return null;
        }
        Optional<UpscaleModel> maybeModel = manager.modelProvider().activeModel();
        if (maybeModel.isEmpty()) {
            return null;
        }
        ResourceMetadata metadata;
        try {
            metadata = resource.metadata();
        } catch (IOException e) {
            return null;
        }
        Optional<AnimationMetadataSection> animation =
                metadata.getSection(AnimationMetadataSection.TYPE);
        if (animation.isPresent() && !ClientEntrypoint.config().upscaleAnimatedTextures()) {
            return null;
        }

        byte[] pngBytes;
        try (InputStream stream = resource.open()) {
            pngBytes = stream.readAllBytes();
        } catch (IOException e) {
            return null;
        }

        String textureId = spriteLocation.toString();
        String namespace = spriteLocation.getNamespace();
        String path = spriteLocation.getPath();

        DetectedTexture dt = manager.detectedTextureRegistry().registerOriginal(
                textureId, namespace, path, pngBytes, original.width(), original.height());

        if (!ClientEntrypoint.config().isTextureEnabled(namespace, textureId)) {
            dt.setDisabled(true);
            dt.status(DetectedTexture.Status.DISABLED);
            return null;
        } else {
            dt.setDisabled(false);
        }

        if (ClientEntrypoint.config().taggedForRegenTextureIds().contains(textureId)) {
            dt.setTaggedForRegen(true);
        }

        UpscaleModel model = maybeModel.get();
        FrameSize originalFrameSize = animation
                .map(value -> value.calculateFrameSize(original.width(), original.height()))
                .orElse(null);
        AnimatedFrameLayout frameLayout = null;
        if (originalFrameSize != null) {
            try {
                frameLayout = AnimatedFrameLayout.of(original.width(), original.height(),
                        originalFrameSize.width(), originalFrameSize.height());
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Rejecting animated sprite {} with invalid frame dimensions", spriteLocation);
                return null;
            }
        }
        boolean animated = frameLayout != null;
        CacheKey key = CacheKey.of(pngBytes, model.name(), model.scaleFactor(), animated);
        Optional<byte[]> cached = manager.cache().lookup(key);
        if (cached.isEmpty()) {
            dt.status(DetectedTexture.Status.QUEUED);
            // Stitching is synchronous; queue for the next reload instead of blocking.
            if (frameLayout == null) {
                manager.queueTexture(spriteLocation.toString(), pngBytes, (id, png) -> { });
            } else {
                manager.queueAnimatedTexture(spriteLocation.toString(), pngBytes,
                        frameLayout.frameWidth(), frameLayout.frameHeight(), (id, png) -> { });
            }
            return null;
        }

        NativeImage upscaledImage;
        try {
            upscaledImage = NativeImage.read(new ByteArrayInputStream(cached.get()));
        } catch (IOException e) {
            LOGGER.warn("Failed to decode cached upscale for {}", spriteLocation, e);
            dt.status(DetectedTexture.Status.FAILED);
            return null;
        }

        manager.detectedTextureRegistry().registerUpscaled(
                textureId, cached.get(), upscaledImage.getWidth(), upscaledImage.getHeight(),
                DetectedTexture.Status.CACHE_HIT);
        if (!SpriteUpscalePolicy.isValidUpscale(original.width(), original.height(),
                upscaledImage.getWidth(), upscaledImage.getHeight(), model.scaleFactor())) {
            LOGGER.warn("Cached upscale for {} has unexpected size {}x{}, expected {}x{}",
                    spriteLocation, upscaledImage.getWidth(), upscaledImage.getHeight(),
                    original.width() * model.scaleFactor(),
                    original.height() * model.scaleFactor());
            upscaledImage.close();
            return null;
        }
        if (frameLayout != null) {
            try {
                AnimatedFrameLayout.of(upscaledImage.getWidth(), upscaledImage.getHeight(),
                        frameLayout.frameWidth() * model.scaleFactor(),
                        frameLayout.frameHeight() * model.scaleFactor());
            } catch (IllegalArgumentException e) {
                upscaledImage.close();
                LOGGER.warn("Cached animated upscale for {} has invalid frame layout", spriteLocation);
                return null;
            }
        }

        Optional<TextureMetadataSection> textureInfo =
                metadata.getSection(TextureMetadataSection.TYPE);
        List<MetadataSectionType.WithValue<?>> additionalMetadata =
                metadata.getTypedSections(additionalMetadataSections);
        FrameSize frameSize = frameLayout == null
                ? new FrameSize(upscaledImage.getWidth(), upscaledImage.getHeight())
                : new FrameSize(frameLayout.frameWidth() * model.scaleFactor(),
                frameLayout.frameHeight() * model.scaleFactor());
        Optional<AnimationMetadataSection> scaledAnimation = animation.map(value ->
                new AnimationMetadataSection(value.frames(),
                        value.frameWidth().map(width -> width * model.scaleFactor()),
                        value.frameHeight().map(height -> height * model.scaleFactor()),
                        value.defaultFrameTime(), value.interpolatedFrames()));
        original.close();
        return new SpriteContents(spriteLocation, frameSize, upscaledImage,
                scaledAnimation, additionalMetadata, textureInfo);
    }
}
