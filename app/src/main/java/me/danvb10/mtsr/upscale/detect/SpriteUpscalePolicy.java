package me.danvb10.mtsr.upscale.detect;

/**
 * Pure decision logic for atlas sprite upscaling. Sprite locations (e.g.
 * {@code modid:block/machine}) name the resource {@code textures/block/machine.png},
 * so eligibility is delegated to {@link TextureDetector} after mapping the
 * sprite path back to its texture path.
 */
public final class SpriteUpscalePolicy {

    private SpriteUpscalePolicy() {
    }

    /** Maps a sprite path (e.g. {@code block/machine}) to its texture resource path. */
    public static String toTexturePath(String spritePath) {
        return "textures/" + spritePath + ".png";
    }

    /** Returns true if the sprite is a mod-provided sprite eligible for upscaling. */
    public static boolean isEligibleSprite(String namespace, String spritePath) {
        if (namespace == null || spritePath == null) {
            return false;
        }
        return TextureDetector.isModTexture(namespace, toTexturePath(spritePath));
    }

    /**
     * Returns true if the upscaled image dimensions are exactly the original
     * dimensions multiplied by the model scale factor, so the sprite's UV
     * mapping stays consistent after stitching.
     */
    public static boolean isValidUpscale(int originalWidth, int originalHeight,
                                         int upscaledWidth, int upscaledHeight, int scaleFactor) {
        if (originalWidth <= 0 || originalHeight <= 0 || scaleFactor <= 0) {
            return false;
        }
        return upscaledWidth == originalWidth * scaleFactor
                && upscaledHeight == originalHeight * scaleFactor;
    }
}
