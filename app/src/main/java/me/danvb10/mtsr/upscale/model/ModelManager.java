package me.danvb10.mtsr.upscale.model;

import me.danvb10.mtsr.config.MtsrConfig;
import me.danvb10.mtsr.config.MtsrConfigStore;
import me.danvb10.mtsr.config.ExecutionProvider;
import me.danvb10.mtsr.upscale.runtime.OnnxRuntimeBootstrap;
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
    private final Path runtimeDirectory;
    private final int tileSize;
    private final int tileOverlap;
    private final MtsrConfig config;
    private final MtsrConfigStore configStore;
    private final ModelLoader modelLoader;
    private UpscaleModel activeModel;
    private boolean loadAttempted;

    public ModelManager(Path modelDirectory) {
        this(modelDirectory, null);
    }

    /**
     * @param runtimeDirectory where the ONNX Runtime jar is downloaded to on
     *                         first use, or {@code null} to skip the runtime
     *                         bootstrap (e.g. in tests).
     */
    public ModelManager(Path modelDirectory, Path runtimeDirectory) {
        this(modelDirectory, runtimeDirectory, MtsrConfig.defaults(), null);
    }

    /** Creates a model manager using tile settings from the supplied config. */
    public ModelManager(Path modelDirectory, Path runtimeDirectory, MtsrConfig config) {
        this(modelDirectory, runtimeDirectory, config, null);
    }

    /** Creates a model manager with persistence for runtime model selection. */
    public ModelManager(Path modelDirectory, Path runtimeDirectory, MtsrConfig config,
                        MtsrConfigStore configStore) {
        this(modelDirectory, runtimeDirectory, config, configStore, null);
    }

    ModelManager(Path modelDirectory, Path runtimeDirectory, MtsrConfig config,
                 MtsrConfigStore configStore, ModelLoader modelLoader) {
        this.modelDirectory = modelDirectory;
        this.runtimeDirectory = runtimeDirectory;
        this.tileSize = config.tileSize();
        this.tileOverlap = config.tileOverlap();
        this.config = config;
        this.configStore = configStore;
        this.modelLoader = modelLoader;
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

    /** Returns the already-loaded model, if any, without triggering a load. */
    public synchronized Optional<UpscaleModel> loadedModel() {
        return Optional.ofNullable(activeModel);
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
        if (runtimeDirectory != null && !OnnxRuntimeBootstrap.ensureAvailable(runtimeDirectory)) {
            return Optional.empty();
        }
        Path modelFile = selectedModelFile();
        return loadModel(modelFile);
    }

    /** Returns the configured model when present, otherwise the first sorted model. */
    public Path selectedModelFile() {
        List<Path> models = availableModels();
        if (models.isEmpty()) {
            return null;
        }
        String configuredName = config.activeModelFileName();
        if (configuredName == null) {
            return models.getFirst();
        }
        for (Path model : models) {
            if (model.getFileName().toString().equals(configuredName)) {
                return model;
            }
        }
        LOGGER.warn("Configured active model '{}' was not found in {}; using automatic selection",
                configuredName, modelDirectory);
        return models.getFirst();
    }

    private synchronized Optional<UpscaleModel> loadModel(Path modelFile) {
        String fileName = modelFile.getFileName().toString();
        String modelName = fileName.substring(0, fileName.length() - ".onnx".length());
        int scale = parseScaleFromName(modelName);
        try {
            activeModel = modelLoader == null
                    ? EsrganModel.load(modelFile, modelName, scale, tileSize, tileOverlap, config)
                    : modelLoader.load(modelFile, modelName, scale,
                    tileSize, tileOverlap, config);
            LOGGER.info("Loaded ESRGAN model '{}' ({}x) from {}", modelName, scale, modelFile);
            return Optional.of(activeModel);
        } catch (ModelExecutionException | UnsatisfiedLinkError | NoClassDefFoundError e) {
            LOGGER.error("Failed to load ESRGAN model {}", modelFile, e);
            return Optional.empty();
        }
    }

    /** Returns the provider actually used by the loaded model. */
    public synchronized ExecutionProvider effectiveExecutionProvider() {
        return activeModel == null ? config.executionProvider() : activeModel.executionProvider();
    }

    /** Switches models, closes the prior session, and persists the selection. */
    public synchronized boolean selectModel(String fileName) {
        Optional<Path> selected = availableModels().stream()
                .filter(path -> path.getFileName().toString().equals(fileName))
                .findFirst();
        if (selected.isEmpty()) {
            LOGGER.warn("Cannot select missing model '{}'", fileName);
            return false;
        }
        if (activeModel != null) {
            activeModel.close();
            activeModel = null;
        }
        loadAttempted = false;
        config.activeModelFileName(fileName);
        persistConfig();
        return activeModel().isPresent();
    }

    private void persistConfig() {
        if (configStore == null) {
            return;
        }
        try {
            configStore.save(config);
        } catch (IOException e) {
            LOGGER.warn("Failed to persist active model selection", e);
        }
    }

    @FunctionalInterface
    interface ModelLoader {
        UpscaleModel load(Path modelFile, String name, int scaleFactor,
                          int tileSize, int tileOverlap, MtsrConfig config)
                throws ModelExecutionException;
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
