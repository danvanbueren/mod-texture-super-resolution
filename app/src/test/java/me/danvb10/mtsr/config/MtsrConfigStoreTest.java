package me.danvb10.mtsr.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MtsrConfigStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void missingFileCreatesDefaults() throws Exception {
        Path file = tempDir.resolve("config/mtsr/config.json");
        MtsrConfig config = new MtsrConfigStore(file).load();

        assertEquals(128, config.tileSize());
        assertTrue(Files.exists(file));
    }

    @Test
    void malformedJsonFallsBackToDefaults() throws Exception {
        Path file = tempDir.resolve("config.json");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "{ definitely not json");

        MtsrConfig config = new MtsrConfigStore(file).load();

        assertEquals(128, config.tileSize());
        assertEquals(ExecutionProvider.CPU, config.executionProvider());
    }

    @Test
    void savesAndLoadsAllSettings() throws Exception {
        Path file = tempDir.resolve("config.json");
        MtsrConfig expected = MtsrConfig.defaults();
        expected.extraExcludedNamespaces(java.util.Set.of("optifine"));
        expected.forceIncludedPaths(java.util.Set.of("somemod:models/"));
        expected.tileSize(256);
        expected.tileOverlap(16);
        expected.workerThreads(1);
        expected.executionProvider(ExecutionProvider.CUDA);
        expected.activeModelFileName("realesrgan-x4.onnx");
        expected.upscaleAnimatedTextures(true);
        expected.showCompletionToast(false);

        MtsrConfigStore store = new MtsrConfigStore(file);
        store.save(expected);
        MtsrConfig actual = store.load();

        assertEquals(expected.extraExcludedNamespaces(), actual.extraExcludedNamespaces());
        assertEquals(expected.forceIncludedPaths(), actual.forceIncludedPaths());
        assertEquals(expected.tileSize(), actual.tileSize());
        assertEquals(expected.tileOverlap(), actual.tileOverlap());
        assertEquals(expected.workerThreads(), actual.workerThreads());
        assertEquals(expected.executionProvider(), actual.executionProvider());
        assertEquals(expected.activeModelFileName(), actual.activeModelFileName());
        assertEquals(expected.upscaleAnimatedTextures(), actual.upscaleAnimatedTextures());
        assertEquals(expected.showCompletionToast(), actual.showCompletionToast());
        assertNotNull(Files.readString(file));
    }
}
