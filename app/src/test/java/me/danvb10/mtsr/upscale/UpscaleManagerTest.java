package me.danvb10.mtsr.upscale;

import me.danvb10.mtsr.config.MtsrConfig;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpscaleManagerTest {

    @TempDir
    Path tempDir;

    /** Deterministic 2x nearest-neighbor stand-in for a real ESRGAN model. */
    private static class FakeModel implements UpscaleModel {
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

    private static final class TrackingModel extends FakeModel {
        private final AtomicInteger calls = new AtomicInteger();
        private final AtomicInteger active = new AtomicInteger();
        private final AtomicInteger maximumActive = new AtomicInteger();
        private final CountDownLatch entered;
        private final CountDownLatch release;

        private TrackingModel() {
            this(null, null);
        }

        private TrackingModel(CountDownLatch entered, CountDownLatch release) {
            this.entered = entered;
            this.release = release;
        }

        @Override
        public int[] upscale(int[] argb, int width, int height) {
            calls.incrementAndGet();
            int concurrent = active.incrementAndGet();
            maximumActive.accumulateAndGet(concurrent, Math::max);
            if (entered != null) {
                entered.countDown();
            }
            try {
                if (release != null) {
                    release.await(10, TimeUnit.SECONDS);
                } else {
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                active.decrementAndGet();
            }
            return super.upscale(argb, width, height);
        }
    }

    private static final class ClosableProvider
            implements me.danvb10.mtsr.upscale.model.ModelProvider, AutoCloseable {
        private final UpscaleModel model;
        private final AtomicBoolean closed = new AtomicBoolean();

        private ClosableProvider(UpscaleModel model) {
            this.model = model;
        }

        @Override
        public Optional<UpscaleModel> activeModel() {
            return Optional.of(model);
        }

        @Override
        public void close() {
            closed.set(true);
        }
    }

    private UpscaleManager newManager() {
        return newManager(new FakeModel(), MtsrConfig.defaults());
    }

    private UpscaleManager newManager(UpscaleModel model, MtsrConfig config) {
        return new UpscaleManager(() -> Optional.of(model),
                new UpscaleCache(tempDir.resolve("cache-" + System.nanoTime())), config);
    }

    private static byte[] testPng() throws IOException {
        return testPng(0xFFFF0000);
    }

    private static byte[] testPng(int firstPixel) throws IOException {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, firstPixel);
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

    @Test
    void configuredPoolProcessesTexturesConcurrently() throws Exception {
        MtsrConfig config = MtsrConfig.defaults();
        config.workerThreads(4);
        TrackingModel model = new TrackingModel();
        CountDownLatch complete = new CountDownLatch(4);

        try (UpscaleManager manager = newManager(model, config)) {
            manager.beginBatch();
            for (int i = 0; i < 4; i++) {
                manager.queueTexture("somemod:textures/item/" + i + ".png",
                        testPng(), (id, bytes) -> complete.countDown());
            }
            manager.endBatch();

            assertTrue(complete.await(10, TimeUnit.SECONDS));
            assertTrue(model.maximumActive.get() > 1);
            assertEquals(4, manager.queuedCount());
            assertEquals(4, manager.upscaledCount());
        }
    }

    @Test
    void deduplicatesSameTextureWhileInferenceIsInFlight() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        TrackingModel model = new TrackingModel(entered, release);
        CountDownLatch callbacks = new CountDownLatch(2);
        byte[] png = testPng();

        try (UpscaleManager manager = newManager(model, MtsrConfig.defaults())) {
            manager.queueTexture("somemod:textures/item/a.png", png,
                    (id, bytes) -> callbacks.countDown());
            assertTrue(entered.await(10, TimeUnit.SECONDS));
            manager.queueTexture("somemod:textures/item/a.png", png,
                    (id, bytes) -> callbacks.countDown());
            release.countDown();

            assertTrue(callbacks.await(10, TimeUnit.SECONDS));
            assertEquals(1, model.calls.get());
            assertEquals(1, manager.queuedCount());
            assertEquals(1, manager.upscaledCount());
        }
    }

    @Test
    void lateJoinAfterResultDrainIsRequeuedAndReceivesCallback() throws Exception {
        MtsrConfig config = MtsrConfig.defaults();
        config.workerThreads(1);
        CountDownLatch firstCallback = new CountDownLatch(1);
        CountDownLatch releaseFirstCallback = new CountDownLatch(1);
        CountDownLatch secondCallback = new CountDownLatch(1);
        TrackingModel model = new TrackingModel();
        byte[] png = testPng();

        try (UpscaleManager manager = newManager(model, config)) {
            manager.queueTexture("somemod:textures/item/a.png", png,
                    (id, bytes) -> {
                        firstCallback.countDown();
                        try {
                            manager.queueTexture("somemod:textures/item/a.png", png,
                                    (duplicateId, duplicateBytes) -> secondCallback.countDown());
                            releaseFirstCallback.await(10, TimeUnit.SECONDS);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    });
            assertTrue(firstCallback.await(10, TimeUnit.SECONDS));
            releaseFirstCallback.countDown();

            assertTrue(secondCallback.await(10, TimeUnit.SECONDS));
            assertEquals(1, model.calls.get());
            assertEquals(2, manager.queuedCount());
            assertEquals(1, manager.upscaledCount());
            assertEquals(1, manager.cacheHitCount());
        }
    }

    @Test
    void completionListenerFiresOnceAndRearmsForNextBatch() throws Exception {
        AtomicInteger notifications = new AtomicInteger();
        CountDownLatch first = new CountDownLatch(1);
        CountDownLatch second = new CountDownLatch(1);

        try (UpscaleManager manager = newManager()) {
            manager.addBatchCompletionListener(() -> {
                if (notifications.incrementAndGet() == 1) {
                    first.countDown();
                } else {
                    second.countDown();
                }
            });
            manager.beginBatch();
            manager.queueTexture("somemod:textures/item/a.png", testPng(),
                    (id, bytes) -> {
                    });
            manager.endBatch();
            assertTrue(first.await(10, TimeUnit.SECONDS));
            assertEquals(1, notifications.get());

            manager.beginBatch();
            manager.queueTexture("somemod:textures/item/b.png", testPng(0xFF010203),
                    (id, bytes) -> {
                    });
            manager.endBatch();
            assertTrue(second.await(10, TimeUnit.SECONDS));
            assertEquals(2, notifications.get());
        }
    }

    @Test
    void completionListenerRespectsConfiguration() throws Exception {
        MtsrConfig config = MtsrConfig.defaults();
        config.showCompletionToast(false);
        AtomicInteger notifications = new AtomicInteger();
        CountDownLatch complete = new CountDownLatch(1);

        try (UpscaleManager manager = newManager(new FakeModel(), config)) {
            manager.addBatchCompletionListener(notifications::incrementAndGet);
            manager.beginBatch();
            manager.queueTexture("somemod:textures/item/a.png", testPng(),
                    (id, bytes) -> complete.countDown());
            manager.endBatch();

            assertTrue(complete.await(10, TimeUnit.SECONDS));
            Thread.sleep(50);
            assertEquals(0, notifications.get());
        }
    }

    @Test
    void missingModelIsSkippedWithoutFailureOrCompletionNotification() throws Exception {
        MtsrConfig config = MtsrConfig.defaults();
        AtomicInteger notifications = new AtomicInteger();
        UpscaleManager manager = new UpscaleManager(Optional::empty,
                new UpscaleCache(tempDir.resolve("no-model-cache")), config);
        manager.addBatchCompletionListener(notifications::incrementAndGet);
        manager.beginBatch();
        manager.queueTexture("somemod:textures/item/a.png", testPng(),
                (id, bytes) -> {
                });
        manager.endBatch();
        long deadline = System.currentTimeMillis() + 10_000;
        while (manager.skippedCount() == 0 && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }

        assertEquals(1, manager.skippedCount());
        assertEquals(0, manager.failedCount());
        assertEquals(0, notifications.get());
        manager.close();
    }

    @Test
    void closeClosesProviderAndSuppressesCompletionNotification() throws Exception {
        ClosableProvider provider = new ClosableProvider(new TrackingModel());
        AtomicInteger notifications = new AtomicInteger();
        UpscaleManager manager = new UpscaleManager(provider,
                new UpscaleCache(tempDir.resolve("close-cache")));
        manager.addBatchCompletionListener(notifications::incrementAndGet);
        manager.beginBatch();
        manager.queueTexture("somemod:textures/item/a.png", testPng(),
                (id, bytes) -> {
                });
        manager.close();

        assertTrue(provider.closed.get());
        assertEquals(0, notifications.get());
    }
}
