package me.danvb10.mtsr.upscale;

import me.danvb10.mtsr.upscale.cache.UpscaleCache;
import me.danvb10.mtsr.upscale.model.UpscaleModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpscaleManagerTest {

    @TempDir
    Path tempDir;

    /** Deterministic 2x nearest-neighbor stand-in for a real ESRGAN model. */
    private static final class FakeModel implements UpscaleModel {
        @Override
        public String name() {
            return "fake-x2";
        }

        @Override
        public int scaleFactor() {
            return 2;
        }

        @Override
        public int[] upscale(int[] argb, int width, int height) {
            int[] out = new int[width * 2 * height * 2];
            for (int y = 0; y < height * 2; y++) {
                for (int x = 0; x < width * 2; x++) {
                    out[y * width * 2 + x] = argb[(y / 2) * width + (x / 2)];
                }
            }
            return out;
        }

        @Override
        public void close() {
        }
    }

    private UpscaleManager newManager() {
        UpscaleModel model = new FakeModel();
        return new UpscaleManager(() -> Optional.of(model),
                new UpscaleCache(tempDir.resolve("cache")));
    }

    private static byte[] testPng() throws IOException {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, 0xFFFF0000);
        image.setRGB(1, 0, 0xFF00FF00);
        image.setRGB(0, 1, 0xFF0000FF);
        image.setRGB(1, 1, 0x80FFFFFF);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    @Test
    void upscalesQueuedTextureAndCachesResult() throws Exception {
        byte[] png = testPng();
        Map<String, byte[]> results = new ConcurrentHashMap<>();

        try (UpscaleManager manager = newManager()) {
            CountDownLatch first = new CountDownLatch(1);
            manager.queueTexture("somemod:textures/item/a.png", png, (id, bytes) -> {
                results.put(id, bytes);
                first.countDown();
            });
            assertTrue(first.await(10, TimeUnit.SECONDS));

            BufferedImage upscaled = ImageIO.read(
                    new ByteArrayInputStream(results.get("somemod:textures/item/a.png")));
            assertEquals(4, upscaled.getWidth());
            assertEquals(4, upscaled.getHeight());
            assertEquals(0xFFFF0000, upscaled.getRGB(0, 0));
            assertEquals(0xFFFF0000, upscaled.getRGB(1, 1));
            assertEquals(1, manager.upscaledCount());
            assertEquals(0, manager.cacheHitCount());

            CountDownLatch second = new CountDownLatch(1);
            manager.queueTexture("somemod:textures/item/a.png", png,
                    (id, bytes) -> second.countDown());
            assertTrue(second.await(10, TimeUnit.SECONDS));
            assertEquals(1, manager.cacheHitCount());
            assertEquals(2, manager.queuedCount());
            assertEquals(0, manager.failedCount());
        }
    }

    @Test
    void countsFailuresForUndecodableInput() throws Exception {
        try (UpscaleManager manager = newManager()) {
            manager.queueTexture("somemod:textures/bad.png", new byte[]{1, 2, 3},
                    (id, bytes) -> {
                    });
            long deadline = System.currentTimeMillis() + 10_000;
            while (manager.failedCount() == 0 && System.currentTimeMillis() < deadline) {
                Thread.sleep(20);
            }
            assertEquals(1, manager.failedCount());
        }
    }
}
