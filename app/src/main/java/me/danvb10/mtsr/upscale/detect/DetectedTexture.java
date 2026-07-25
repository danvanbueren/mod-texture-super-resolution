package me.danvb10.mtsr.upscale.detect;

import java.util.Arrays;

/**
 * Metadata and image data for a texture detected by the super-resolution pipeline.
 */
public final class DetectedTexture {

    public enum Status {
        QUEUED("Queued"),
        UPSCALED("Upscaled"),
        CACHE_HIT("Cache Hit"),
        DISABLED("Disabled"),
        FAILED("Failed"),
        SKIPPED("Skipped");

        private final String displayName;

        Status(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }
    }

    private final String textureId;
    private final String namespace;
    private final String path;
    private byte[] originalPng;
    private int originalWidth;
    private int originalHeight;
    private byte[] upscaledPng;
    private int upscaledWidth;
    private int upscaledHeight;
    private Status status;
    private boolean disabled;
    private boolean taggedForRegen;
    private String modelName;
    private String errorMessage;

    public DetectedTexture(String textureId, String namespace, String path,
                           byte[] originalPng, int originalWidth, int originalHeight) {
        this.textureId = textureId;
        this.namespace = namespace;
        this.path = path;
        this.originalPng = originalPng;
        this.originalWidth = originalWidth;
        this.originalHeight = originalHeight;
        this.status = Status.QUEUED;
    }

    public String textureId() {
        return textureId;
    }

    public String namespace() {
        return namespace;
    }

    public String path() {
        return path;
    }

    public byte[] originalPng() {
        return originalPng != null ? Arrays.copyOf(originalPng, originalPng.length) : null;
    }

    public void originalPng(byte[] originalPng, int width, int height) {
        this.originalPng = originalPng != null ? Arrays.copyOf(originalPng, originalPng.length) : null;
        this.originalWidth = width;
        this.originalHeight = height;
    }

    public int originalWidth() {
        return originalWidth;
    }

    public int originalHeight() {
        return originalHeight;
    }

    public byte[] upscaledPng() {
        return upscaledPng != null ? Arrays.copyOf(upscaledPng, upscaledPng.length) : null;
    }

    public void upscaledPng(byte[] upscaledPng, int width, int height) {
        this.upscaledPng = upscaledPng != null ? Arrays.copyOf(upscaledPng, upscaledPng.length) : null;
        this.upscaledWidth = width;
        this.upscaledHeight = height;
    }

    public int upscaledWidth() {
        return upscaledWidth;
    }

    public int upscaledHeight() {
        return upscaledHeight;
    }

    public Status status() {
        return status;
    }

    public void status(Status status) {
        this.status = status;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public boolean isTaggedForRegen() {
        return taggedForRegen;
    }

    public void setTaggedForRegen(boolean taggedForRegen) {
        this.taggedForRegen = taggedForRegen;
    }

    public String modelName() {
        return modelName;
    }

    public void modelName(String modelName) {
        this.modelName = modelName;
    }

    public String errorMessage() {
        return errorMessage;
    }

    public void errorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
