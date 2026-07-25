package me.danvb10.mtsr.upscale;

import me.danvb10.mtsr.config.MtsrConfig;
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
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

/**
 * Orchestrates the upscale pipeline: for each detected mod texture it checks
 * the disk cache, runs the ESRGAN model on a cache miss, stores the result,
 * and hands the upscaled PNG to a consumer for registration with the game.
 * All work happens on a background daemon thread so the render thread is
 * never blocked.
 */
public final class UpscaleManager implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger("mtsr");

    private final ModelProvider modelProvider;
    private final UpscaleCache cache;
    private final ExecutorService executor;
    private final MtsrConfig config;

    private final AtomicInteger queued = new AtomicInteger();
    private final AtomicInteger upscaled = new AtomicInteger();
    private final AtomicInteger cacheHits = new AtomicInteger();
    private final AtomicInteger failed = new AtomicInteger();

    public UpscaleManager(ModelProvider modelProvider, UpscaleCache cache) {
        this(modelProvider, cache, MtsrConfig.defaults());
    }

    /** Creates a manager using the supplied persistent configuration. */
    public UpscaleManager(ModelProvider modelProvider, UpscaleCache cache, MtsrConfig config) {
        this.modelProvider = modelProvider;
        this.cache = cache;
        this.config = config;
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "mtsr-upscale-worker");
            thread.setDaemon(true);
            thread.setPriority(Thread.MIN_PRIORITY);
            return thread;
        });
    }

    /** Creates a manager rooted at the game directory's standard mod paths. */
    public static UpscaleManager create(Path gameDirectory) {
        return create(gameDirectory, MtsrConfig.defaults());
    }

    /** Creates a manager rooted at the game directory using its configuration. */
    public static UpscaleManager create(Path gameDirectory, MtsrConfig config) {
        ModelManager models = new ModelManager(
                gameDirectory.resolve("config/mtsr/models"),
                gameDirectory.resolve("config/mtsr/runtime"), config);
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
        queued.incrementAndGet();
        executor.execute(() -> process(textureId, pngBytes, onUpscaled));
    }

    private void process(String textureId, byte[] pngBytes,
                         BiConsumer<String, byte[]> onUpscaled) {
        Optional<UpscaleModel> maybeModel = modelProvider.activeModel();
        if (maybeModel.isEmpty()) {
            return;
        }
        UpscaleModel model = maybeModel.get();
        CacheKey key = CacheKey.of(pngBytes, model.name(), model.scaleFactor());
        Optional<byte[]> cached = cache.lookup(key);
        if (cached.isPresent()) {
            cacheHits.incrementAndGet();
            onUpscaled.accept(textureId, cached.get());
            return;
        }
        try {
            byte[] result = upscalePng(model, pngBytes);
            cache.store(key, result);
            upscaled.incrementAndGet();
            onUpscaled.accept(textureId, result);
        } catch (IOException | ModelExecutionException | RuntimeException e) {
            failed.incrementAndGet();
            LOGGER.warn("Failed to upscale texture {}", textureId, e);
        }
    }

    private static byte[] upscalePng(UpscaleModel model, byte[] pngBytes)
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
        return bytes.toByteArray();
    }

    public ModelProvider modelProvider() {
        return modelProvider;
    }

    public UpscaleCache cache() {
        return cache;
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

    @Override
    public void close() {
        executor.shutdownNow();
        if (modelProvider instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception e) {
                LOGGER.warn("Failed to close model provider", e);
            }
        }
    }
}
