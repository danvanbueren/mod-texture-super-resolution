package me.danvb10.mtsr.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class MtsrConfigTest {

    @Test
    void defaultsUsePipelineDefaults() {
        MtsrConfig config = MtsrConfig.defaults();

        assertEquals(128, config.tileSize());
        assertEquals(8, config.tileOverlap());
        assertEquals(Math.max(1, Runtime.getRuntime().availableProcessors() / 2),
                config.workerThreads());
        assertEquals(ExecutionProvider.CPU, config.executionProvider());
        assertNull(config.activeModelFileName());
        assertEquals(false, config.upscaleAnimatedTextures());
        assertEquals(true, config.showCompletionToast());
    }

    @Test
    void validationClampsUnsafeValues() {
        MtsrConfig config = new MtsrConfig();
        config.tileSize(1);
        config.tileOverlap(9999);
        config.workerThreads(Integer.MAX_VALUE);
        config.executionProvider(null);
        config.activeModelFileName(" ");

        config.validate();

        assertEquals(MtsrConfig.MIN_TILE_SIZE, config.tileSize());
        assertEquals(config.tileSize() / 2, config.tileOverlap());
        assertEquals(Math.max(1, Runtime.getRuntime().availableProcessors()),
                config.workerThreads());
        assertEquals(ExecutionProvider.CPU, config.executionProvider());
        assertNull(config.activeModelFileName());
    }

    @Test
    void setsAreCopiedAndSanitizedOnValidation() {
        MtsrConfig config = new MtsrConfig();
        config.extraExcludedNamespaces(java.util.Set.of(" optifine ", ""));
        config.forceIncludedPaths(java.util.Set.of("mod:path"));
        config.validate();

        assertEquals(java.util.Set.of("optifine"), config.extraExcludedNamespaces());
        assertEquals(java.util.Set.of("mod:path"), config.forceIncludedPaths());
    }
}
