package me.danvb10.mtsr.upscale.model;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import me.danvb10.mtsr.config.ExecutionProvider;
import me.danvb10.mtsr.config.MtsrConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.FloatBuffer;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ESRGAN super-resolution model backed by an ONNX Runtime session.
 *
 * <p>Expects a Real-ESRGAN style graph: single input of shape [1, 3, H, W]
 * with RGB values normalized to 0..1, single output of shape
 * [1, 3, H * scale, W * scale]. Large images are processed in overlapping
 * tiles to bound memory usage; the alpha channel is upscaled separately with
 * nearest-neighbor sampling to preserve cutout transparency.</p>
 *
 * <p>Inference calls are serialized per model because the ONNX Runtime session
 * is not assumed safe for concurrent calls in this setup.</p>
 */
public final class EsrganModel implements UpscaleModel {

    private static final Logger LOGGER = LoggerFactory.getLogger("mtsr");
    private static final Map<OrtEnvironment, Integer> ENVIRONMENT_REFERENCES =
            new IdentityHashMap<>();
    private static final Set<ExecutionProvider> FALLBACK_LOGGED =
            ConcurrentHashMap.newKeySet();

    private final String name;
    private final int scaleFactor;
    private final int tileSize;
    private final int tileOverlap;
    private final OrtEnvironment environment;
    private final OrtSession session;
    private final String inputName;
    private final ExecutionProvider executionProvider;
    private final Object inferenceLock = new Object();
    private boolean closed;

    private EsrganModel(String name, int scaleFactor, int tileSize, int tileOverlap,
                        OrtEnvironment environment, OrtSession session, String inputName,
                        ExecutionProvider executionProvider) {
        this.name = name;
        this.scaleFactor = scaleFactor;
        this.tileSize = tileSize;
        this.tileOverlap = tileOverlap;
        this.environment = environment;
        this.session = session;
        this.inputName = inputName;
        this.executionProvider = executionProvider;
    }

    /** Loads an ESRGAN ONNX model from disk. */
    public static EsrganModel load(Path modelFile, String name, int scaleFactor,
                                   int tileSize, int tileOverlap) throws ModelExecutionException {
        return load(modelFile, name, scaleFactor, tileSize, tileOverlap, MtsrConfig.defaults());
    }

    /** Loads a model using the requested execution provider where available. */
    public static EsrganModel load(Path modelFile, String name, int scaleFactor,
                                   int tileSize, int tileOverlap, MtsrConfig config)
            throws ModelExecutionException {
        OrtEnvironment environment = acquireEnvironment();
        ExecutionProvider requested = config.executionProvider();
        try {
            SessionSetup setup = createSession(environment, modelFile, requested);
            String inputName = setup.session().getInputNames().iterator().next();
            return new EsrganModel(name, scaleFactor, tileSize, tileOverlap,
                    environment, setup.session(), inputName, setup.provider());
        } catch (OrtException e) {
            releaseEnvironment(environment);
            throw new ModelExecutionException("Failed to load ONNX model " + modelFile, e);
        } catch (RuntimeException e) {
            releaseEnvironment(environment);
            throw e;
        }
    }

    private static SessionSetup createSession(OrtEnvironment environment, Path modelFile,
                                              ExecutionProvider requested)
            throws OrtException {
        OrtSession.SessionOptions options = null;
        try {
            ConfiguredOptions configured = optionsFor(requested);
            options = configured.options();
            OrtSession session = environment.createSession(modelFile.toString(), options);
            options.close();
            return new SessionSetup(session, configured.provider());
        } catch (Throwable failure) {
            if (options != null) {
                options.close();
            }
            if (requested == ExecutionProvider.CPU) {
                if (failure instanceof OrtException ortException) {
                    throw ortException;
                }
                if (failure instanceof Error error) {
                    throw error;
                }
                throw new RuntimeException(failure);
            }
            if (FALLBACK_LOGGED.add(requested)) {
                LOGGER.warn("ONNX execution provider {} failed during session creation; "
                        + "falling back to CPU", requested, failure);
            }
            try (OrtSession.SessionOptions fallbackOptions =
                         optionsFor(ExecutionProvider.CPU).options()) {
                return new SessionSetup(
                        environment.createSession(modelFile.toString(), fallbackOptions),
                        ExecutionProvider.CPU);
            }
        }
    }

    static ConfiguredOptions optionsFor(ExecutionProvider provider) throws OrtException {
        OrtSession.SessionOptions options = new OrtSession.SessionOptions();
        options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);
        if (provider == ExecutionProvider.CPU) {
            return new ConfiguredOptions(options, ExecutionProvider.CPU);
        }
        try {
            switch (provider) {
                case CUDA -> options.addCUDA();
                case DIRECTML -> options.addDirectML(0);
                case CPU -> {
                }
            }
            return new ConfiguredOptions(options, provider);
        } catch (Throwable failure) {
            options.close();
            if (FALLBACK_LOGGED.add(provider)) {
                LOGGER.warn("ONNX execution provider {} unavailable; falling back to CPU",
                        provider, failure);
            }
            OrtSession.SessionOptions cpuOptions = new OrtSession.SessionOptions();
            cpuOptions.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);
            return new ConfiguredOptions(cpuOptions, ExecutionProvider.CPU);
        }
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public int scaleFactor() {
        return scaleFactor;
    }

    @Override
    public int[] upscale(int[] argb, int width, int height) throws ModelExecutionException {
        synchronized (inferenceLock) {
            return upscaleLocked(argb, width, height);
        }
    }

    @Override
    public ExecutionProvider executionProvider() {
        return executionProvider;
    }

    private int[] upscaleLocked(int[] argb, int width, int height) throws ModelExecutionException {
        int[] result = new int[width * scaleFactor * height * scaleFactor];
        if (width <= tileSize && height <= tileSize) {
            int[] upscaled = runInference(argb, width, height);
            System.arraycopy(upscaled, 0, result, 0, upscaled.length);
        } else {
            List<Tiling.Tile> tiles = Tiling.computeTiles(width, height, tileSize, tileOverlap);
            for (Tiling.Tile tile : tiles) {
                int[] tilePixels = Tiling.extract(argb, width, tile);
                int[] upscaledTile = runInference(tilePixels, tile.width(), tile.height());
                Tiling.stitch(upscaledTile, tile, scaleFactor, result, width * scaleFactor);
            }
        }
        ImageOps.upscaleAlphaNearest(argb, width, height, result, scaleFactor);
        return result;
    }

    private int[] runInference(int[] argb, int width, int height) throws ModelExecutionException {
        synchronized (inferenceLock) {
            float[] input = ImageOps.argbToChwRgb(argb, width, height);
            long[] shape = {1, 3, height, width};
            try (OnnxTensor tensor = OnnxTensor.createTensor(environment, FloatBuffer.wrap(input), shape);
                 OrtSession.Result outputs = session.run(Map.of(inputName, tensor))) {
                OnnxTensor output = (OnnxTensor) outputs.get(0);
                FloatBuffer buffer = output.getFloatBuffer();
                float[] chw = new float[buffer.remaining()];
                buffer.get(chw);
                int outWidth = width * scaleFactor;
                int outHeight = height * scaleFactor;
                if (chw.length != 3 * outWidth * outHeight) {
                    throw new ModelExecutionException("Unexpected output size " + chw.length
                            + " for " + outWidth + "x" + outHeight + " (model scale mismatch?)");
                }
                return ImageOps.chwRgbToArgb(chw, outWidth, outHeight);
            } catch (OrtException e) {
                throw new ModelExecutionException("Inference failed for " + width + "x" + height, e);
            }
        }
    }

    private record SessionSetup(OrtSession session, ExecutionProvider provider) {
    }

    record ConfiguredOptions(OrtSession.SessionOptions options, ExecutionProvider provider) {
    }

    @Override
    public void close() {
        synchronized (inferenceLock) {
            if (closed) {
                return;
            }
            closed = true;
            try {
                session.close();
            } catch (Exception e) {
                LOGGER.warn("Failed to close ONNX session for {}", name, e);
            } finally {
                releaseEnvironment(environment);
            }
        }
    }

    private static synchronized OrtEnvironment acquireEnvironment() {
        OrtEnvironment environment = OrtEnvironment.getEnvironment();
        ENVIRONMENT_REFERENCES.merge(environment, 1, Integer::sum);
        return environment;
    }

    private static synchronized void releaseEnvironment(OrtEnvironment environment) {
        Integer references = ENVIRONMENT_REFERENCES.get(environment);
        if (references == null || references <= 1) {
            ENVIRONMENT_REFERENCES.remove(environment);
            try {
                environment.close();
            } catch (RuntimeException e) {
                LOGGER.warn("Failed to close ONNX environment", e);
            }
        } else {
            ENVIRONMENT_REFERENCES.put(environment, references - 1);
        }
    }
}
