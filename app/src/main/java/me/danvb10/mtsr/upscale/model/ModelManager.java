package me.danvb10.mtsr.upscale.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Discovers and loads ESRGAN ONNX models from the mod's model directory
 * ({@code config/mtsr/models}). The active model is loaded lazily on first
 * use and reused for all subsequent upscales.
 *
 * <p>The upscale factor is parsed from the file name: a model named like
 * {@code realesrgan-x4.onnx} is treated as 4x. Files without an
 * {@code -xN} / {@code _xN} suffix default to {@value #DEFAULT_SCALE}x.</p>
 */
public final class ModelManager implements ModelProvider, AutoCloseable {

    public static final int DEFAULT_SCALE = 4;
    public static final int DEFAULT_TILE_SIZE = 128;
    public static final int DEFAULT_TILE_OVERLAP = 8;

    private static final Logger LOGGER = LoggerFactory.getLogger("mtsr");
    private static final Pattern SCALE_SUFFIX =
            Pattern.compile("[-_]x(\\d+)$", Pattern.CASE_INSENSITIVE);

    private final Path modelDirectory;
    private UpscaleModel activeModel;
    private boolean loadAttempted;

    public ModelManager(Path modelDirectory) {
        this.modelDirectory = modelDirectory;
    }

    /** Lists available .onnx model files, sorted by name. */
    public List<Path> availableModels() {
        if (!Files.isDirectory(modelDirectory)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(modelDirectory)) {
            return files
                    .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".onnx"))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            LOGGER.warn("Failed to list model directory {}", modelDirectory, e);
            return List.of();
        }
    }

    /**
     * Returns the active model, loading the first available one on demand.
     * Empty if no model file is present or loading failed.
     */
    public synchronized Optional<UpscaleModel> activeModel() {
        if (activeModel != null) {
            return Optional.of(activeModel);
        }
        if (loadAttempted) {
            return Optional.empty();
        }
        loadAttempted = true;
        try {
            Files.createDirectories(modelDirectory);
        } catch (IOException e) {
            LOGGER.warn("Failed to create model directory {}", modelDirectory, e);
        }
        List<Path> models = availableModels();
        if (models.isEmpty()) {
            LOGGER.info("No ESRGAN model found in {}. Place a Real-ESRGAN .onnx model "
                    + "(e.g. realesrgan-x4.onnx) there to enable texture upscaling.", modelDirectory);
            return Optional.empty();
        }
        Path modelFile = models.getFirst();
        String fileName = modelFile.getFileName().toString();
        String modelName = fileName.substring(0, fileName.length() - ".onnx".length());
        int scale = parseScaleFromName(modelName);
        try {
            activeModel = EsrganModel.load(modelFile, modelName, scale,
                    DEFAULT_TILE_SIZE, DEFAULT_TILE_OVERLAP);
            LOGGER.info("Loaded ESRGAN model '{}' ({}x) from {}", modelName, scale, modelFile);
            return Optional.of(activeModel);
        } catch (ModelExecutionException | UnsatisfiedLinkError | NoClassDefFoundError e) {
            LOGGER.error("Failed to load ESRGAN model {}", modelFile, e);
            return Optional.empty();
        }
    }

    /** Parses the trailing -xN / _xN scale suffix from a model name. */
    public static int parseScaleFromName(String modelName) {
        Matcher matcher = SCALE_SUFFIX.matcher(modelName);
        if (matcher.find()) {
            int scale = Integer.parseInt(matcher.group(1));
            if (scale >= 1 && scale <= 16) {
                return scale;
            }
        }
        return DEFAULT_SCALE;
    }

    @Override
    public synchronized void close() {
        if (activeModel != null) {
            activeModel.close();
            activeModel = null;
        }
        loadAttempted = false;
    }
}
