package me.danvb10.mtsr.upscale.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

/**
 * Disk cache for upscaled textures. Entries are PNG files named by their
 * {@link CacheKey}, so lookups are a single file existence check and stale
 * entries are never served (a changed source texture yields a new key).
 */
public final class UpscaleCache {

    private static final Logger LOGGER = LoggerFactory.getLogger("mtsr");

    private final Path cacheDirectory;

    public UpscaleCache(Path cacheDirectory) {
        this.cacheDirectory = cacheDirectory;
    }

    /** Returns the cached upscaled PNG for the key, if present. */
    public Optional<byte[]> lookup(CacheKey key) {
        Path file = cacheDirectory.resolve(key.fileName());
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readAllBytes(file));
        } catch (IOException e) {
            LOGGER.warn("Failed to read cache entry {}", file, e);
            return Optional.empty();
        }
    }

    /** Atomically stores an upscaled PNG under the key. */
    public void store(CacheKey key, byte[] pngBytes) {
        try {
            Files.createDirectories(cacheDirectory);
            Path target = cacheDirectory.resolve(key.fileName());
            Path temp = Files.createTempFile(cacheDirectory, "mtsr-", ".tmp");
            Files.write(temp, pngBytes);
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            LOGGER.warn("Failed to store cache entry {}", key.fileName(), e);
        }
    }

    /** Deletes all cache entries. Returns the number of files removed. */
    public int clear() {
        if (!Files.isDirectory(cacheDirectory)) {
            return 0;
        }
        int removed = 0;
        try (var files = Files.list(cacheDirectory)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                try {
                    Files.delete(file);
                    removed++;
                } catch (IOException e) {
                    LOGGER.warn("Failed to delete cache entry {}", file, e);
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to clear cache directory {}", cacheDirectory, e);
        }
        return removed;
    }
}
