package me.danvb10.mtsr.upscale.model;

import me.danvb10.mtsr.config.MtsrConfig;
import me.danvb10.mtsr.config.MtsrConfigStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelManagerTest {

    @TempDir
    Path tempDir;

    @Test
    void parsesScaleSuffixFromModelNames() {
        assertEquals(4, ModelManager.parseScaleFromName("realesrgan-x4"));
        assertEquals(2, ModelManager.parseScaleFromName("realesrgan_x2"));
        assertEquals(8, ModelManager.parseScaleFromName("esrgan-X8"));
        assertEquals(ModelManager.DEFAULT_SCALE, ModelManager.parseScaleFromName("mymodel"));
        assertEquals(ModelManager.DEFAULT_SCALE, ModelManager.parseScaleFromName("model-x99"));
    }

    @Test
    void listsOnlyOnnxFilesSorted() throws IOException {
        Files.createDirectories(tempDir);
        Files.writeString(tempDir.resolve("b-model.onnx"), "");
        Files.writeString(tempDir.resolve("a-model.onnx"), "");
        Files.writeString(tempDir.resolve("readme.txt"), "");
        try (ModelManager manager = new ModelManager(tempDir)) {
            List<Path> models = manager.availableModels();
            assertEquals(2, models.size());
            assertEquals("a-model.onnx", models.get(0).getFileName().toString());
            assertEquals("b-model.onnx", models.get(1).getFileName().toString());
        }
    }

    @Test
    void activeModelEmptyWhenNoModelPresent() {
        try (ModelManager manager = new ModelManager(tempDir.resolve("models"))) {
            assertTrue(manager.activeModel().isEmpty());
            assertTrue(Files.isDirectory(tempDir.resolve("models")),
                    "model directory should be created for the user");
        }
    }

    @Test
    void configuredModelIsPreferredAndMissingOneFallsBack() throws IOException {
        Files.createDirectories(tempDir);
        Files.writeString(tempDir.resolve("a-x2.onnx"), "");
        Files.writeString(tempDir.resolve("b-x4.onnx"), "");
        MtsrConfig config = MtsrConfig.defaults();
        config.activeModelFileName("b-x4.onnx");
        try (ModelManager manager = new ModelManager(tempDir, null, config, null)) {
            assertEquals("b-x4.onnx", manager.selectedModelFile().getFileName().toString());
            config.activeModelFileName("missing.onnx");
            assertEquals("a-x2.onnx", manager.selectedModelFile().getFileName().toString());
        }
    }

    @Test
    void switchingModelClosesPreviousAndPersistsSelection() throws Exception {
        Files.createDirectories(tempDir);
        Files.writeString(tempDir.resolve("a-x2.onnx"), "");
        Files.writeString(tempDir.resolve("b-x4.onnx"), "");
        MtsrConfig config = MtsrConfig.defaults();
        MtsrConfigStore store = new MtsrConfigStore(tempDir.resolve("config.json"));
        AtomicReference<FakeModel> previous = new AtomicReference<>();
        ModelManager.ModelLoader loader = (path, name, scale, tile, overlap, ignored) -> {
            FakeModel model = new FakeModel(name, scale);
            previous.set(model);
            return model;
        };

        try (ModelManager manager = new ModelManager(tempDir, null, config, store, loader)) {
            assertTrue(manager.activeModel().isPresent());
            FakeModel first = previous.get();
            assertTrue(manager.selectModel("b-x4.onnx"));
            assertTrue(first.closed);
            assertEquals("b-x4.onnx", store.load().activeModelFileName());
        }
    }

    private static final class FakeModel implements UpscaleModel {
        private final String name;
        private final int scale;
        private boolean closed;

        private FakeModel(String name, int scale) {
            this.name = name;
            this.scale = scale;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public int scaleFactor() {
            return scale;
        }

        @Override
        public int[] upscale(int[] argb, int width, int height) {
            return argb;
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}
