package me.danvb10.mtsr.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MtsrConfigControllerTest {

    @TempDir
    Path tempDir;

    @Test
    void mutationsValidateAndPersistImmediately() {
        MtsrConfig config = MtsrConfig.defaults();
        MtsrConfigStore store = new MtsrConfigStore(tempDir.resolve("config.json"));
        MtsrConfigController controller = new MtsrConfigController(config, store);

        assertTrue(controller.update(value -> {
            value.tileSize(1);
            value.tileOverlap(9999);
            value.upscaleAnimatedTextures(true);
        }));
        MtsrConfig loaded = store.load();
        assertEquals(MtsrConfig.MIN_TILE_SIZE, loaded.tileSize());
        assertEquals(MtsrConfig.MIN_TILE_SIZE / 2, loaded.tileOverlap());
        assertTrue(loaded.upscaleAnimatedTextures());
    }

    @Test
    void namespaceMutationsTrimAndPersist() {
        MtsrConfig config = MtsrConfig.defaults();
        MtsrConfigStore store = new MtsrConfigStore(tempDir.resolve("config.json"));
        MtsrConfigController controller = new MtsrConfigController(config, store);

        assertTrue(controller.addExcludedNamespace("  example  "));
        assertEquals(java.util.Set.of("example"), store.load().extraExcludedNamespaces());
        assertTrue(controller.removeExcludedNamespace("example"));
        assertTrue(store.load().extraExcludedNamespaces().isEmpty());
    }
}
