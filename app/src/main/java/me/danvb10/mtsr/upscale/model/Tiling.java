package me.danvb10.mtsr.upscale.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits large images into overlapping tiles so inference stays within memory
 * bounds, and stitches results back together discarding the overlap seams.
 */
public final class Tiling {

    /** A source-space tile plus the region of it that survives stitching. */
    public record Tile(int x, int y, int width, int height,
                       int innerX, int innerY, int innerWidth, int innerHeight) {
    }

    private Tiling() {
    }

    /**
     * Computes overlapping tiles covering a width x height image.
     *
     * @param tileSize maximum tile edge length in source pixels
     * @param overlap  number of source pixels tiles share on interior edges
     */
    public static List<Tile> computeTiles(int width, int height, int tileSize, int overlap) {
        if (tileSize <= 0) {
            throw new IllegalArgumentException("tileSize must be positive");
        }
        if (overlap < 0 || overlap * 2 >= tileSize) {
            throw new IllegalArgumentException("overlap must be >= 0 and < tileSize / 2");
        }
        List<Tile> tiles = new ArrayList<>();
        int step = tileSize - 2 * overlap;
        for (int innerY = 0; innerY < height; innerY += step) {
            int innerH = Math.min(step, height - innerY);
            int y0 = Math.max(0, innerY - overlap);
            int y1 = Math.min(height, innerY + innerH + overlap);
            for (int innerX = 0; innerX < width; innerX += step) {
                int innerW = Math.min(step, width - innerX);
                int x0 = Math.max(0, innerX - overlap);
                int x1 = Math.min(width, innerX + innerW + overlap);
                tiles.add(new Tile(x0, y0, x1 - x0, y1 - y0,
                        innerX - x0, innerY - y0, innerW, innerH));
            }
        }
        return tiles;
    }

    /** Copies a tile out of the full source image. */
    public static int[] extract(int[] source, int sourceWidth, Tile tile) {
        int[] out = new int[tile.width() * tile.height()];
        for (int row = 0; row < tile.height(); row++) {
            System.arraycopy(source, (tile.y() + row) * sourceWidth + tile.x(),
                    out, row * tile.width(), tile.width());
        }
        return out;
    }

    /**
     * Writes the inner (non-overlap) region of an upscaled tile into the full
     * upscaled destination image.
     */
    public static void stitch(int[] upscaledTile, Tile tile, int scale,
                              int[] dest, int destWidth) {
        int tileW = tile.width() * scale;
        int srcX = tile.innerX() * scale;
        int srcY = tile.innerY() * scale;
        int copyW = tile.innerWidth() * scale;
        int copyH = tile.innerHeight() * scale;
        int destX = (tile.x() + tile.innerX()) * scale;
        int destY = (tile.y() + tile.innerY()) * scale;
        for (int row = 0; row < copyH; row++) {
            System.arraycopy(upscaledTile, (srcY + row) * tileW + srcX,
                    dest, (destY + row) * destWidth + destX, copyW);
        }
    }
}
