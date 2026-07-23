package me.danvb10.mtsr.upscale.cache;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpscaleCacheTest {

    @TempDir
    Path tempDir;

    @Test
    void lookupMissesOnEmptyCache() {
        UpscaleCache cache = new UpscaleCache(tempDir.resolve("cache"));
        assertTrue(cache.lookup(CacheKey.of(new byte[]{1}, "m", 4)).isEmpty());
    }

    @Test
    void storeThenLookupRoundTrips() {
        UpscaleCache cache = new UpscaleCache(tempDir.resolve("cache"));
        CacheKey key = CacheKey.of(new byte[]{1, 2}, "m", 4);
        byte[] payload = {9, 8, 7};
        cache.store(key, payload);
        Optional<byte[]> result = cache.lookup(key);
        assertTrue(result.isPresent());
        assertArrayEquals(payload, result.get());
    }

    @Test
    void clearRemovesAllEntries() {
        UpscaleCache cache = new UpscaleCache(tempDir.resolve("cache"));
        cache.store(CacheKey.of(new byte[]{1}, "m", 4), new byte[]{1});
        cache.store(CacheKey.of(new byte[]{2}, "m", 4), new byte[]{2});
        assertEquals(2, cache.clear());
        assertTrue(cache.lookup(CacheKey.of(new byte[]{1}, "m", 4)).isEmpty());
    }
}
