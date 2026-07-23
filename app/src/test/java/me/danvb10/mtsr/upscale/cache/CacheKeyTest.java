package me.danvb10.mtsr.upscale.cache;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CacheKeyTest {

    private static final byte[] BYTES_A = {1, 2, 3, 4};
    private static final byte[] BYTES_B = {1, 2, 3, 5};

    @Test
    void deterministicForSameInputs() {
        assertEquals(CacheKey.of(BYTES_A, "model", 4), CacheKey.of(BYTES_A, "model", 4));
    }

    @Test
    void differsWhenAnyComponentChanges() {
        CacheKey base = CacheKey.of(BYTES_A, "model", 4);
        assertNotEquals(base, CacheKey.of(BYTES_B, "model", 4));
        assertNotEquals(base, CacheKey.of(BYTES_A, "other", 4));
        assertNotEquals(base, CacheKey.of(BYTES_A, "model", 2));
    }

    @Test
    void fileNameIsHexHashWithPngExtension() {
        CacheKey key = CacheKey.of(BYTES_A, "model", 4);
        assertTrue(key.fileName().matches("[0-9a-f]{64}\\.png"));
    }
}
