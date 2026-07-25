package me.danvb10.mtsr.config;

import me.danvb10.mtsr.upscale.model.ModelManager;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Persistent settings for the texture super-resolution pipeline.
 *
 * <p>The model is deliberately independent of Minecraft classes so it can be
 * loaded and validated in unit tests.</p>
 */
public final class MtsrConfig {

    public static final int MIN_TILE_SIZE = 32;
    public static final int MAX_TILE_SIZE = 1024;
    public static final int MIN_THREADS = 1;

    private Set<String> extraExcludedNamespaces = new LinkedHashSet<>();
    private Set<String> forceIncludedPaths = new LinkedHashSet<>();
    private Set<String> disabledNamespaces = new LinkedHashSet<>();
    private Set<String> disabledTextureIds = new LinkedHashSet<>();
    private Set<String> taggedForRegenTextureIds = new LinkedHashSet<>();
    private int tileSize = ModelManager.DEFAULT_TILE_SIZE;
    private int tileOverlap = ModelManager.DEFAULT_TILE_OVERLAP;
    private int workerThreads = defaultWorkerThreads();
    private ExecutionProvider executionProvider = ExecutionProvider.CPU;
    private String activeModelFileName;
    private boolean upscaleAnimatedTextures;
    private boolean showCompletionToast = true;

    /** Creates a configuration populated with default values. */
    public MtsrConfig() {
    }

    /** Returns a new default configuration. */
    public static MtsrConfig defaults() {
        return new MtsrConfig();
    }

    /** Validates and clamps all values loaded from external data. */
    public MtsrConfig validate() {
        extraExcludedNamespaces = sanitize(extraExcludedNamespaces);
        forceIncludedPaths = sanitize(forceIncludedPaths);
        disabledNamespaces = sanitize(disabledNamespaces);
        disabledTextureIds = sanitize(disabledTextureIds);
        taggedForRegenTextureIds = sanitize(taggedForRegenTextureIds);
        tileSize = clamp(tileSize, MIN_TILE_SIZE, MAX_TILE_SIZE);
        tileOverlap = clamp(tileOverlap, 0, tileSize / 2);
        int processors = Runtime.getRuntime().availableProcessors();
        workerThreads = clamp(workerThreads, MIN_THREADS, Math.max(MIN_THREADS, processors));
        if (executionProvider == null) {
            executionProvider = ExecutionProvider.CPU;
        }
        if (activeModelFileName != null && activeModelFileName.isBlank()) {
            activeModelFileName = null;
        }
        return this;
    }

    /** Returns user-configured namespaces excluded from upscaling. */
    public Set<String> extraExcludedNamespaces() {
        return Set.copyOf(extraExcludedNamespaces);
    }

    /** Sets user-configured namespaces excluded from upscaling. */
    public void extraExcludedNamespaces(Set<String> namespaces) {
        extraExcludedNamespaces = new LinkedHashSet<>(namespaces == null ? Set.of() : namespaces);
    }

    /** Returns resource path prefixes forced into the upscale pipeline. */
    public Set<String> forceIncludedPaths() {
        return Set.copyOf(forceIncludedPaths);
    }

    /** Sets resource path prefixes forced into the upscale pipeline. */
    public void forceIncludedPaths(Set<String> paths) {
        forceIncludedPaths = new LinkedHashSet<>(paths == null ? Set.of() : paths);
    }

    /** Returns the model inference tile size in pixels. */
    public int tileSize() {
        return tileSize;
    }

    /** Sets the model inference tile size in pixels. */
    public void tileSize(int tileSize) {
        this.tileSize = tileSize;
    }

    /** Returns the model inference tile overlap in pixels. */
    public int tileOverlap() {
        return tileOverlap;
    }

    /** Sets the model inference tile overlap in pixels. */
    public void tileOverlap(int tileOverlap) {
        this.tileOverlap = tileOverlap;
    }

    /** Returns the configured background worker count. */
    public int workerThreads() {
        return workerThreads;
    }

    /** Sets the configured background worker count. */
    public void workerThreads(int workerThreads) {
        this.workerThreads = workerThreads;
    }

    /** Returns the configured model execution provider. */
    public ExecutionProvider executionProvider() {
        return executionProvider;
    }

    /** Sets the configured model execution provider. */
    public void executionProvider(ExecutionProvider executionProvider) {
        this.executionProvider = executionProvider;
    }

    /** Returns the selected model filename, or null for automatic selection. */
    public String activeModelFileName() {
        return activeModelFileName;
    }

    /** Sets the selected model filename, or null for automatic selection. */
    public void activeModelFileName(String activeModelFileName) {
        this.activeModelFileName = activeModelFileName;
    }

    /** Returns whether animated textures should be upscaled. */
    public boolean upscaleAnimatedTextures() {
        return upscaleAnimatedTextures;
    }

    /** Sets whether animated textures should be upscaled. */
    public void upscaleAnimatedTextures(boolean upscaleAnimatedTextures) {
        this.upscaleAnimatedTextures = upscaleAnimatedTextures;
    }

    /** Returns whether completion notifications should be shown. */
    public boolean showCompletionToast() {
        return showCompletionToast;
    }

    /** Sets whether completion notifications should be shown. */
    public void showCompletionToast(boolean showCompletionToast) {
        this.showCompletionToast = showCompletionToast;
    }

    /** Returns group-level disabled namespaces. */
    public Set<String> disabledNamespaces() {
        return Set.copyOf(disabledNamespaces);
    }

    /** Sets group-level disabled namespaces. */
    public void disabledNamespaces(Set<String> namespaces) {
        this.disabledNamespaces = new LinkedHashSet<>(namespaces == null ? Set.of() : namespaces);
    }

    /** Returns individually disabled texture IDs. */
    public Set<String> disabledTextureIds() {
        return Set.copyOf(disabledTextureIds);
    }

    /** Sets individually disabled texture IDs. */
    public void disabledTextureIds(Set<String> textureIds) {
        this.disabledTextureIds = new LinkedHashSet<>(textureIds == null ? Set.of() : textureIds);
    }

    /** Returns texture IDs tagged for regeneration. */
    public Set<String> taggedForRegenTextureIds() {
        return Set.copyOf(taggedForRegenTextureIds);
    }

    /** Sets texture IDs tagged for regeneration. */
    public void taggedForRegenTextureIds(Set<String> textureIds) {
        this.taggedForRegenTextureIds = new LinkedHashSet<>(textureIds == null ? Set.of() : textureIds);
    }

    /** Returns true if a texture is enabled (not disabled by namespace or texture ID). */
    public boolean isTextureEnabled(String namespace, String textureId) {
        if (namespace != null && disabledNamespaces.contains(namespace)) {
            return false;
        }
        if (textureId != null && disabledTextureIds.contains(textureId)) {
            return false;
        }
        return true;
    }

    /** Sets group namespace enablement. */
    public void setNamespaceEnabled(String namespace, boolean enabled) {
        if (namespace == null) return;
        if (enabled) {
            disabledNamespaces.remove(namespace);
        } else {
            disabledNamespaces.add(namespace);
        }
    }

    /** Sets individual texture enablement. */
    public void setTextureEnabled(String textureId, boolean enabled) {
        if (textureId == null) return;
        if (enabled) {
            disabledTextureIds.remove(textureId);
        } else {
            disabledTextureIds.add(textureId);
        }
    }

    /** Toggles or sets tag for regeneration on a texture ID. */
    public void setTextureTaggedForRegen(String textureId, boolean tagged) {
        if (textureId == null) return;
        if (tagged) {
            taggedForRegenTextureIds.add(textureId);
        } else {
            taggedForRegenTextureIds.remove(textureId);
        }
    }

    private static int defaultWorkerThreads() {
        return Math.max(MIN_THREADS, Runtime.getRuntime().availableProcessors() / 2);
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static Set<String> sanitize(Set<String> values) {
        Set<String> sanitized = new LinkedHashSet<>();
        if (values != null) {
            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    sanitized.add(value.trim());
                }
            }
        }
        return sanitized;
    }
}
