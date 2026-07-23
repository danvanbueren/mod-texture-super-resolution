package me.danvb10.mtsr.upscale.model;

/** Pure pixel-format conversions shared by model implementations. */
public final class ImageOps {

    private ImageOps() {
    }

    /**
     * Converts packed ARGB pixels to a normalized (0..1) float tensor in CHW order
     * with 3 channels (RGB). Alpha is dropped; see {@link #upscaleAlphaNearest}.
     */
    public static float[] argbToChwRgb(int[] argb, int width, int height) {
        int plane = width * height;
        float[] chw = new float[3 * plane];
        for (int i = 0; i < plane; i++) {
            int p = argb[i];
            chw[i] = ((p >> 16) & 0xFF) / 255.0f;
            chw[plane + i] = ((p >> 8) & 0xFF) / 255.0f;
            chw[2 * plane + i] = (p & 0xFF) / 255.0f;
        }
        return chw;
    }

    /**
     * Converts a normalized CHW RGB float tensor back to packed ARGB pixels,
     * clamping each channel to [0, 1] and setting alpha to fully opaque.
     */
    public static int[] chwRgbToArgb(float[] chw, int width, int height) {
        int plane = width * height;
        int[] argb = new int[plane];
        for (int i = 0; i < plane; i++) {
            int r = clampToByte(chw[i]);
            int g = clampToByte(chw[plane + i]);
            int b = clampToByte(chw[2 * plane + i]);
            argb[i] = 0xFF000000 | (r << 16) | (g << 8) | b;
        }
        return argb;
    }

    /**
     * Upscales only the alpha channel of the source image with nearest-neighbor
     * sampling and applies it to the (already upscaled) RGB pixels in place.
     */
    public static void upscaleAlphaNearest(int[] source, int srcWidth, int srcHeight,
                                           int[] target, int scale) {
        int dstWidth = srcWidth * scale;
        int dstHeight = srcHeight * scale;
        for (int y = 0; y < dstHeight; y++) {
            int sy = y / scale;
            for (int x = 0; x < dstWidth; x++) {
                int sx = x / scale;
                int alpha = source[sy * srcWidth + sx] >>> 24;
                int i = y * dstWidth + x;
                target[i] = (alpha << 24) | (target[i] & 0x00FFFFFF);
            }
        }
    }

    private static int clampToByte(float v) {
        int i = Math.round(v * 255.0f);
        return Math.max(0, Math.min(255, i));
    }
}
