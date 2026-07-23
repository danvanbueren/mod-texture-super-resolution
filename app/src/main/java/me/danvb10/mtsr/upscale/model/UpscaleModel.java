package me.danvb10.mtsr.upscale.model;

/**
 * A super-resolution model that upscales raster images by a fixed integer factor.
 * Pixel data is exchanged as ARGB packed integers in row-major order.
 */
public interface UpscaleModel extends AutoCloseable {

    /** Unique, filesystem-safe name of this model (used in cache keys). */
    String name();

    /** The fixed integer upscale factor of this model (e.g. 4 for a 4x ESRGAN). */
    int scaleFactor();

    /**
     * Upscales the given image.
     *
     * @param argb   source pixels, packed ARGB, row-major, length == width * height
     * @param width  source width in pixels
     * @param height source height in pixels
     * @return upscaled pixels of size (width * scaleFactor()) x (height * scaleFactor())
     * @throws ModelExecutionException if inference fails
     */
    int[] upscale(int[] argb, int width, int height) throws ModelExecutionException;

    @Override
    void close();
}
