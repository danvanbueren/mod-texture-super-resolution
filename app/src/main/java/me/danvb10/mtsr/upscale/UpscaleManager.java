package me.danvb10.mtsr.upscale;

import me.danvb10.mtsr.config.MtsrConfig;
import me.danvb10.mtsr.config.MtsrConfigStore;
import me.danvb10.mtsr.upscale.cache.CacheKey;
import me.danvb10.mtsr.upscale.cache.UpscaleCache;
import me.danvb10.mtsr.upscale.model.ModelExecutionException;
import me.danvb10.mtsr.upscale.model.ModelManager;
import me.danvb10.mtsr.upscale.model.ModelProvider;
import me.danvb10.mtsr.upscale.model.UpscaleModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

/**
 * Orchestrates the upscale pipeline: for each detected mod texture it checks
 * the disk cache, runs the ESRGAN model on a cache miss, stores the result,
 * and hands the upscaled PNG to a consumer for registration with the game.
 * All work happens on background daemon threads so the render thread is never
 * blocked.
 */
public final class UpscaleManager implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger("mtsr");
    private static final DateTimeFormatter LOG_TIME =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ModelProvider modelProvider;
    private final UpscaleCache cache;
    private final ExecutorService executor;
    private final MtsrConfig config;
    private final ActivityLogBuffer activityLog = new ActivityLogBuffer(300);
    private final Map<WorkKey, List<BiConsumer<String, byte[]>>> inFlight =
            new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<Runnable> batchCompletionListeners =
            new CopyOnWriteArrayList<>();
    private final Object batchMonitor = new Object();
    private final AtomicInteger workerId = new AtomicInteger();
    private volatile boolean closed;
    private BatchState currentBatch;

    private final AtomicInteger queued = new AtomicInteger();
    private final AtomicInteger upscaled = new AtomicInteger();
    private final AtomicInteger cacheHits = new AtomicInteger();
    private final AtomicInteger failed = new AtomicInteger();
    private final AtomicInteger skipped = new AtomicInteger();

    public UpscaleManager(ModelProvider modelProvider, UpscaleCache cache) {
        this(modelProvider, cache, MtsrConfig.defaults());
    }

    /** Creates a manager using the supplied persistent configuration. */
    public UpscaleManager(ModelProvider modelProvider, UpscaleCache cache, MtsrConfig config) {
        this.modelProvider = modelProvider;
        this.cache = cache;
        this.config = config;
        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable,
                    "mtsr-upscale-worker-" + workerId.incrementAndGet());
            thread.setDaemon(true);
            thread.setPriority(Thread.MIN_PRIORITY);
            return thread;
        };
        this.executor = Executors.newFixedThreadPool(config.workerThreads(), threadFactory);
    }

    /** Creates a manager rooted at the game directory's standard mod paths. */
    public static UpscaleManager create(Path gameDirectory) {
        return create(gameDirectory, MtsrConfig.defaults());
    }

    /** Creates a manager rooted at the game directory using its configuration. */
    public static UpscaleManager create(Path gameDirectory, MtsrConfig config) {
        ModelManager models = new ModelManager(
                gameDirectory.resolve("config/mtsr/models"),
                gameDirectory.resolve("config/mtsr/runtime"), config,
                new MtsrConfigStore(gameDirectory.resolve("config/mtsr/config.json")));
        UpscaleCache cache = new UpscaleCache(gameDirectory.resolve("mtsr/cache"));
        return new UpscaleManager(models, cache, config);
    }

    /**
     * Queues a texture for upscaling. The consumer is invoked from the worker
     * thread with the texture id and the upscaled PNG bytes on success; it is
     * responsible for hopping to the render thread before touching GL state.
     */
    public void queueTexture(String textureId, byte[] pngBytes,
                             BiConsumer<String, byte[]> onUpscaled) {
        queueInternal(textureId, pngBytes, onUpscaled, null);
    }

    /** Queues an animated texture for independent per-frame upscaling. */
    public void queueAnimatedTexture(String textureId, byte[] pngBytes,
                                     int frameWidth, int frameHeight,
                                     BiConsumer<String, byte[]> onUpscaled) {
        queueInternal(textureId, pngBytes, onUpscaled,
                new AnimationSpec(frameWidth, frameHeight));
    }

    private void queueInternal(String textureId, byte[] pngBytes,
                               BiConsumer<String, byte[]> onUpscaled,
                               AnimationSpec animation) {
        if (closed) {
            return;
        }
        WorkKey workKey = WorkKey.of(textureId, pngBytes, animation);
        List<BiConsumer<String, byte[]>> callbacks = new CopyOnWriteArrayList<>();
        callbacks.add(onUpscaled);
        AtomicBoolean owner = new AtomicBoolean();
        inFlight.compute(workKey, (key, existing) -> {
            if (existing == null) {
                owner.set(true);
                return callbacks;
            }
            existing.add(onUpscaled);
            return existing;
        });
        if (!owner.get()) {
            return;
        }
        BatchState batch;
        synchronized (batchMonitor) {
            if (currentBatch == null || currentBatch.ended) {
                currentBatch = new BatchState();
                currentBatch.ended = true;
            }
            batch = currentBatch;
            batch.pending++;
            if (batch.implicit) {
                currentBatch = null;
            }
        }
        queued.incrementAndGet();
        try {
            executor.execute(() -> process(textureId, pngBytes, workKey, batch, animation));
        } catch (RuntimeException e) {
            inFlight.remove(workKey);
            finish(batch, false);
            throw e;
        }
    }

    /** Starts an explicit batch for resource reload work. */
    public void beginBatch() {
        synchronized (batchMonitor) {
            currentBatch = new BatchState();
            currentBatch.implicit = false;
        }
    }

    /** Marks the current explicit batch complete once queued work drains. */
    public void endBatch() {
        BatchState batch;
        synchronized (batchMonitor) {
            batch = currentBatch;
            if (batch == null || batch.implicit) {
                return;
            }
            batch.ended = true;
            currentBatch = null;
        }
        maybeNotify(batch);
    }

    /** Registers a Minecraft-free callback invoked after a successful batch. */
    public void addBatchCompletionListener(Runnable listener) {
        batchCompletionListeners.add(listener);
    }

    /** Removes a previously registered batch completion callback. */
    public void removeBatchCompletionListener(Runnable listener) {
        batchCompletionListeners.remove(listener);
    }

    private void process(String textureId, byte[] pngBytes, WorkKey workKey,
                         BatchState batch, AnimationSpec animation) {
        boolean success = false;
        try {
            Optional<UpscaleModel> maybeModel = modelProvider.activeModel();
            if (maybeModel.isEmpty()) {
                skipped.incrementAndGet();
                activityLog.append(logMessage("Skipped " + textureId + " (no active model)"));
                return;
            }
            UpscaleModel model = maybeModel.get();
            CacheKey key = CacheKey.of(pngBytes, model.name(), model.scaleFactor(),
                    animation != null);
            long started = System.nanoTime();
            Optional<byte[]> cached = cache.lookup(key);
            if (cached.isPresent()) {
                cacheHits.incrementAndGet();
                notifyCallbacks(workKey, textureId, cached.get());
                Dimensions source = pngDimensions(pngBytes);
                Dimensions target = pngDimensions(cached.get());
                if (source != null && target != null) {
                    activityLog.append(logMessage(formatActivity("Cache hit", textureId,
                            source, target, elapsedMillis(started))));
                } else {
                    activityLog.append(logMessage("Cache hit " + textureId
                            + " in " + elapsedMillis(started) + "ms"));
                }
                return;
            }
            UpscaleResult result = animation == null
                    ? upscalePng(model, pngBytes)
                    : upscaleAnimatedPng(model, pngBytes, animation);
            cache.store(key, result.bytes());
            upscaled.incrementAndGet();
            success = true;
            notifyCallbacks(workKey, textureId, result.bytes());
            activityLog.append(logMessage(formatActivity("Upscaled", textureId,
                    result.source(), result.target(), elapsedMillis(started))));
        } catch (IOException | ModelExecutionException | RuntimeException e) {
            failed.incrementAndGet();
            LOGGER.warn("Failed to upscale texture {}", textureId, e);
            activityLog.append(logMessage("Failed " + textureId + ": " + e.getMessage()));
        } finally {
            if (!success) {
                inFlight.remove(workKey);
            }
            finish(batch, success);
        }
    }

    private void notifyCallbacks(WorkKey workKey, String textureId, byte[] pngBytes) {
        AtomicReference<List<BiConsumer<String, byte[]>>> drained = new AtomicReference<>();
        inFlight.computeIfPresent(workKey, (key, callbacks) -> {
            drained.set(callbacks);
            return null;
        });
        List<BiConsumer<String, byte[]>> callbacks = drained.get();
        if (callbacks == null) {
            return;
        }
        for (BiConsumer<String, byte[]> callback : callbacks) {
            try {
                callback.accept(textureId, pngBytes);
            } catch (RuntimeException e) {
                LOGGER.warn("Upscale callback failed for {}", textureId, e);
            }
        }
    }

    private void finish(BatchState batch, boolean success) {
        synchronized (batchMonitor) {
            batch.pending--;
            if (success) {
                batch.upscaled++;
            }
        }
        maybeNotify(batch);
    }

    private void maybeNotify(BatchState batch) {
        synchronized (batchMonitor) {
            if (closed || !batch.ended || batch.pending != 0
                    || batch.upscaled == 0 || batch.notified) {
                return;
            }
            batch.notified = true;
        }
        if (!config.showCompletionToast()) {
            return;
        }
        for (Runnable listener : batchCompletionListeners) {
            try {
                listener.run();
            } catch (RuntimeException e) {
                LOGGER.warn("Batch completion listener failed", e);
            }
        }
    }

    private static UpscaleResult upscalePng(UpscaleModel model, byte[] pngBytes)
            throws IOException, ModelExecutionException {
        BufferedImage source = ImageIO.read(new ByteArrayInputStream(pngBytes));
        if (source == null) {
            throw new IOException("Not a decodable image");
        }
        int width = source.getWidth();
        int height = source.getHeight();
        int[] argb = source.getRGB(0, 0, width, height, null, 0, width);
        int[] result = model.upscale(argb, width, height);
        int scale = model.scaleFactor();
        BufferedImage out = new BufferedImage(width * scale, height * scale,
                BufferedImage.TYPE_INT_ARGB);
        out.setRGB(0, 0, width * scale, height * scale, result, 0, width * scale);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(out, "png", bytes);
        return new UpscaleResult(bytes.toByteArray(),
                new Dimensions(width, height),
                new Dimensions(width * scale, height * scale));
    }

    private static UpscaleResult upscaleAnimatedPng(UpscaleModel model, byte[] pngBytes,
                                                    AnimationSpec animation)
            throws IOException, ModelExecutionException {
        BufferedImage source = ImageIO.read(new ByteArrayInputStream(pngBytes));
        if (source == null) {
            throw new IOException("Not a decodable image");
        }
        AnimatedFrameLayout layout;
        try {
            layout = AnimatedFrameLayout.of(source.getWidth(), source.getHeight(),
                    animation.frameWidth(), animation.frameHeight());
        } catch (IllegalArgumentException e) {
            throw new IOException(e.getMessage(), e);
        }
        int scale = model.scaleFactor();
        BufferedImage out = new BufferedImage(layout.outputWidth(scale),
                layout.outputHeight(scale), BufferedImage.TYPE_INT_ARGB);
        for (int row = 0; row < layout.rows(); row++) {
            for (int column = 0; column < layout.columns(); column++) {
                int[] argb = source.getRGB(column * layout.frameWidth(),
                        row * layout.frameHeight(), layout.frameWidth(),
                        layout.frameHeight(), null, 0, layout.frameWidth());
                int[] result = model.upscale(argb, layout.frameWidth(), layout.frameHeight());
                out.setRGB(column * layout.frameWidth() * scale,
                        row * layout.frameHeight() * scale,
                        layout.frameWidth() * scale, layout.frameHeight() * scale,
                        result, 0, layout.frameWidth() * scale);
            }
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(out, "png", bytes);
        return new UpscaleResult(bytes.toByteArray(),
                new Dimensions(source.getWidth(), source.getHeight()),
                new Dimensions(out.getWidth(), out.getHeight()));
    }

    private static Dimensions pngDimensions(byte[] pngBytes) {
        if (pngBytes.length < 24
                || pngBytes[0] != (byte) 0x89 || pngBytes[1] != 0x50
                || pngBytes[2] != 0x4E || pngBytes[3] != 0x47
                || pngBytes[4] != 0x0D || pngBytes[5] != 0x0A
                || pngBytes[6] != 0x1A || pngBytes[7] != 0x0A) {
            return null;
        }
        int width = readInt(pngBytes, 16);
        int height = readInt(pngBytes, 20);
        return width > 0 && height > 0 ? new Dimensions(width, height) : null;
    }

    private static int readInt(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 24)
                | ((bytes[offset + 1] & 0xFF) << 16)
                | ((bytes[offset + 2] & 0xFF) << 8)
                | (bytes[offset + 3] & 0xFF);
    }

    private static String formatActivity(String action, String textureId,
                                         Dimensions source, Dimensions target, long millis) {
        return action + " " + textureId + " (" + source.width() + "x" + source.height()
                + " -> " + target.width() + "x" + target.height() + ") in " + millis + "ms";
    }

    private static String logMessage(String message) {
        return "[" + LocalTime.now().format(LOG_TIME) + "] " + message;
    }

    private static long elapsedMillis(long started) {
        return Duration.ofNanos(System.nanoTime() - started).toMillis();
    }

    public ModelProvider modelProvider() {
        return modelProvider;
    }

    public UpscaleCache cache() {
        return cache;
    }

    /** Returns the bounded, thread-safe activity log. */
    public ActivityLogBuffer activityLog() {
        return activityLog;
    }

    /** Returns the configuration used by this pipeline. */
    public MtsrConfig config() {
        return config;
    }

    public int queuedCount() {
        return queued.get();
    }

    public int upscaledCount() {
        return upscaled.get();
    }

    public int cacheHitCount() {
        return cacheHits.get();
    }

    public int failedCount() {
        return failed.get();
    }

    /** Returns the number of textures skipped because no model was available. */
    public int skippedCount() {
        return skipped.get();
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        executor.shutdownNow();
        try {
            if (!executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                LOGGER.warn("Upscale workers did not terminate within five seconds");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.warn("Interrupted while stopping upscale workers", e);
        }
        if (modelProvider instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception e) {
                LOGGER.warn("Failed to close model provider", e);
            }
        }
    }

    private static final class BatchState {
        private int pending;
        private int upscaled;
        private boolean implicit = true;
        private boolean ended;
        private boolean notified;
    }

    private record WorkKey(String textureId, String sourceHash,
                           AnimationSpec animation) {
        private static WorkKey of(String textureId, byte[] pngBytes, AnimationSpec animation) {
            return new WorkKey(textureId, CacheKey.contentHash(pngBytes), animation);
        }
    }

    private record AnimationSpec(int frameWidth, int frameHeight) {
    }

    private record Dimensions(int width, int height) {
    }

    private record UpscaleResult(byte[] bytes, Dimensions source, Dimensions target) {
    }
}
