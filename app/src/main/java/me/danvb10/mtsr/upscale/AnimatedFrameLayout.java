package me.danvb10.mtsr.upscale;

/**
 * Validates and describes a regular animation frame grid.
 * Frames are ordered row-major, matching the vanilla strip/grid convention.
 */
public record AnimatedFrameLayout(int imageWidth, int imageHeight,
                                  int frameWidth, int frameHeight,
                                  int columns, int rows) {

    /** Computes a layout, rejecting dimensions that cannot divide into whole frames. */
    public static AnimatedFrameLayout of(int imageWidth, int imageHeight,
                                         int frameWidth, int frameHeight) {
        if (imageWidth <= 0 || imageHeight <= 0 || frameWidth <= 0 || frameHeight <= 0
                || imageWidth % frameWidth != 0 || imageHeight % frameHeight != 0) {
            throw new IllegalArgumentException("Animation frames must divide image dimensions exactly");
        }
        return new AnimatedFrameLayout(imageWidth, imageHeight, frameWidth, frameHeight,
                imageWidth / frameWidth, imageHeight / frameHeight);
    }

    /** Returns the number of frames in the grid. */
    public int frameCount() {
        return columns * rows;
    }

    /** Returns the upscaled image width for the supplied model factor. */
    public int outputWidth(int scaleFactor) {
        return imageWidth * scaleFactor;
    }

    /** Returns the upscaled image height for the supplied model factor. */
    public int outputHeight(int scaleFactor) {
        return imageHeight * scaleFactor;
    }
}
