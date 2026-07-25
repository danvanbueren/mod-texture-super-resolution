package me.danvb10.mtsr.upscale.detect;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe registry holding all detected textures and their upscaled equivalents,
 * organized by mod/namespace.
 */
public final class DetectedTextureRegistry {

    private final Map<String, Map<String, DetectedTexture>> registry = new ConcurrentHashMap<>();

    public DetectedTextureRegistry() {
    }

    /**
     * Registers or updates an original texture entry in the registry.
     */
    public DetectedTexture registerOriginal(String textureId, String namespace, String path,
                                             byte[] pngBytes, int width, int height) {
        Map<String, DetectedTexture> namespaceMap =
                registry.computeIfAbsent(namespace, k -> new ConcurrentHashMap<>());
        return namespaceMap.compute(textureId, (id, existing) -> {
            if (existing == null) {
                return new DetectedTexture(textureId, namespace, path, pngBytes, width, height);
            }
            existing.originalPng(pngBytes, width, height);
            return existing;
        });
    }

    /**
     * Updates an upscaled texture entry.
     */
    public void registerUpscaled(String textureId, byte[] upscaledPng, int width, int height,
                                 DetectedTexture.Status status) {
        DetectedTexture texture = findTexture(textureId);
        if (texture != null) {
            texture.upscaledPng(upscaledPng, width, height);
            texture.status(status);
        }
    }

    /**
     * Updates status or error message of a texture.
     */
    public void updateStatus(String textureId, DetectedTexture.Status status, String errorMessage) {
        DetectedTexture texture = findTexture(textureId);
        if (texture != null) {
            texture.status(status);
            if (errorMessage != null) {
                texture.errorMessage(errorMessage);
            }
        }
    }

    /**
     * Finds a texture by ID across all namespaces.
     */
    public DetectedTexture findTexture(String textureId) {
        int colonIndex = textureId.indexOf(':');
        if (colonIndex != -1) {
            String namespace = textureId.substring(0, colonIndex);
            Map<String, DetectedTexture> map = registry.get(namespace);
            if (map != null && map.containsKey(textureId)) {
                return map.get(textureId);
            }
        }
        for (Map<String, DetectedTexture> map : registry.values()) {
            if (map.containsKey(textureId)) {
                return map.get(textureId);
            }
        }
        return null;
    }

    /**
     * Returns an unmodifiable set of all registered namespaces sorted alphabetically.
     */
    public Set<String> getNamespaces() {
        Set<String> namespaces = new TreeSet<>(registry.keySet());
        return Collections.unmodifiableSet(namespaces);
    }

    /**
     * Returns all textures belonging to a specific namespace.
     */
    public List<DetectedTexture> getTexturesByNamespace(String namespace) {
        Map<String, DetectedTexture> map = registry.get(namespace);
        if (map == null) {
            return List.of();
        }
        List<DetectedTexture> list = new ArrayList<>(map.values());
        list.sort(Comparator.comparing(DetectedTexture::textureId));
        return Collections.unmodifiableList(list);
    }

    /**
     * Returns a snapshot map of all namespaces to lists of textures.
     */
    public Map<String, List<DetectedTexture>> getAllTextures() {
        Map<String, List<DetectedTexture>> result = new LinkedHashMap<>();
        for (String namespace : getNamespaces()) {
            result.put(namespace, getTexturesByNamespace(namespace));
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * Sets disabled state for all textures in a namespace group.
     */
    public void setNamespaceDisabled(String namespace, boolean disabled) {
        Map<String, DetectedTexture> map = registry.get(namespace);
        if (map != null) {
            for (DetectedTexture texture : map.values()) {
                texture.setDisabled(disabled);
                if (disabled) {
                    texture.status(DetectedTexture.Status.DISABLED);
                }
            }
        }
    }

    /**
     * Sets disabled state for an individual texture.
     */
    public void setTextureDisabled(String textureId, boolean disabled) {
        DetectedTexture texture = findTexture(textureId);
        if (texture != null) {
            texture.setDisabled(disabled);
            if (disabled) {
                texture.status(DetectedTexture.Status.DISABLED);
            }
        }
    }

    /**
     * Sets tag for regeneration for an individual texture.
     */
    public void setTextureTaggedForRegen(String textureId, boolean tagged) {
        DetectedTexture texture = findTexture(textureId);
        if (texture != null) {
            texture.setTaggedForRegen(tagged);
        }
    }

    /**
     * Returns all textures that have been tagged for regeneration.
     */
    public List<DetectedTexture> getTaggedTextures() {
        List<DetectedTexture> tagged = new ArrayList<>();
        for (Map<String, DetectedTexture> map : registry.values()) {
            for (DetectedTexture texture : map.values()) {
                if (texture.isTaggedForRegen()) {
                    tagged.add(texture);
                }
            }
        }
        return Collections.unmodifiableList(tagged);
    }

    public int totalCount() {
        return registry.values().stream().mapToInt(Map::size).sum();
    }

    public int upscaledCount() {
        int count = 0;
        for (Map<String, DetectedTexture> map : registry.values()) {
            for (DetectedTexture t : map.values()) {
                if (t.status() == DetectedTexture.Status.UPSCALED || t.status() == DetectedTexture.Status.CACHE_HIT) {
                    count++;
                }
            }
        }
        return count;
    }

    public int disabledCount() {
        int count = 0;
        for (Map<String, DetectedTexture> map : registry.values()) {
            for (DetectedTexture t : map.values()) {
                if (t.isDisabled()) {
                    count++;
                }
            }
        }
        return count;
    }

    public int taggedCount() {
        return getTaggedTextures().size();
    }

    public void clear() {
        registry.clear();
    }
}
