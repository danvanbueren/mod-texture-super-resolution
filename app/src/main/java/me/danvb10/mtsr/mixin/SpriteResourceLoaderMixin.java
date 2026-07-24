package me.danvb10.mtsr.mixin;

import me.danvb10.mtsr.upscale.AtlasSpriteUpscaler;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

/**
 * Wraps the sprite resource loader used for atlas stitching so mod-provided
 * sprites can be replaced by their upscaled versions before the atlas is
 * stitched.
 */
@Mixin(SpriteResourceLoader.class)
public interface SpriteResourceLoaderMixin {

    @Inject(method = "create", at = @At("RETURN"), cancellable = true)
    private static void mtsr$wrapLoader(Set<MetadataSectionType<?>> additionalMetadataSections,
                                        CallbackInfoReturnable<SpriteResourceLoader> cir) {
        cir.setReturnValue(
                AtlasSpriteUpscaler.wrap(cir.getReturnValue(), additionalMetadataSections));
    }
}
