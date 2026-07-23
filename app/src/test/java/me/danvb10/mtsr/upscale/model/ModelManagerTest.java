package me.danvb10.mtsr.upscale.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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
}
