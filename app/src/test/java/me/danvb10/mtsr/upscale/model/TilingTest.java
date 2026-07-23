package me.danvb10.mtsr.upscale.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TilingTest {

    @Test
    void singleTileWhenImageFits() {
        List<Tiling.Tile> tiles = Tiling.computeTiles(64, 32, 128, 8);
        assertEquals(1, tiles.size());
        Tiling.Tile tile = tiles.getFirst();
        assertEquals(0, tile.x());
        assertEquals(0, tile.y());
        assertEquals(64, tile.width());
        assertEquals(32, tile.height());
        assertEquals(64, tile.innerWidth());
        assertEquals(32, tile.innerHeight());
    }

    @Test
    void tilesCoverEntireImageExactlyOnce() {
        int width = 300;
        int height = 200;
        List<Tiling.Tile> tiles = Tiling.computeTiles(width, height, 128, 8);
        int[] coverage = new int[width * height];
        for (Tiling.Tile tile : tiles) {
            for (int dy = 0; dy < tile.innerHeight(); dy++) {
                for (int dx = 0; dx < tile.innerWidth(); dx++) {
                    coverage[(tile.y() + tile.innerY() + dy) * width
                            + (tile.x() + tile.innerX() + dx)]++;
                }
            }
        }
        for (int count : coverage) {
            assertEquals(1, count, "every pixel must be produced by exactly one tile");
        }
    }

    @Test
    void tilesStayWithinBoundsAndTileSize() {
        List<Tiling.Tile> tiles = Tiling.computeTiles(500, 137, 128, 8);
        for (Tiling.Tile tile : tiles) {
            assertTrue(tile.x() >= 0 && tile.y() >= 0);
            assertTrue(tile.x() + tile.width() <= 500);
            assertTrue(tile.y() + tile.height() <= 137);
            assertTrue(tile.width() <= 128);
            assertTrue(tile.height() <= 128);
        }
    }

    @Test
    void extractAndStitchRoundTrip() {
        int width = 20;
        int height = 12;
        int[] source = new int[width * height];
        for (int i = 0; i < source.length; i++) {
            source[i] = i;
        }
        int scale = 2;
        int[] dest = new int[width * scale * height * scale];
        for (Tiling.Tile tile : Tiling.computeTiles(width, height, 8, 2)) {
            int[] tilePixels = Tiling.extract(source, width, tile);
            int[] upscaled = nearestUpscale(tilePixels, tile.width(), tile.height(), scale);
            Tiling.stitch(upscaled, tile, scale, dest, width * scale);
        }
        int[] expected = nearestUpscale(source, width, height, scale);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], dest[i], "pixel " + i);
        }
    }

    @Test
    void rejectsInvalidArguments() {
        assertThrows(IllegalArgumentException.class, () -> Tiling.computeTiles(10, 10, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> Tiling.computeTiles(10, 10, 8, 4));
        assertThrows(IllegalArgumentException.class, () -> Tiling.computeTiles(10, 10, 8, -1));
    }

    private static int[] nearestUpscale(int[] src, int w, int h, int scale) {
        int[] out = new int[w * scale * h * scale];
        for (int y = 0; y < h * scale; y++) {
            for (int x = 0; x < w * scale; x++) {
                out[y * w * scale + x] = src[(y / scale) * w + (x / scale)];
            }
        }
        return out;
    }
}
