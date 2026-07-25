package me.danvb10.mtsr.upscale.detect;

import me.danvb10.mtsr.config.MtsrConfig;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Decides which resource-pack textures were loaded by (non-vanilla) mods and
 * are therefore candidates for upscaling. Operates on plain namespace/path
 * strings so it stays independent of Minecraft classes and easy to test.
 */
public final class TextureDetector {

    /** Namespaces that are never upscaled: vanilla content and this mod itself. */
    public static final Set<String> EXCLUDED_NAMESPACES = Set.of("minecraft", "realms", "mtsr");

    private TextureDetector() {
    }

    /**
     * Returns true if the resource identified by namespace and path is a
     * mod-provided PNG texture eligible for upscaling.
     */
    public static boolean isModTexture(String namespace, String path) {
        return isModTexture(namespace, path, MtsrConfig.defaults());
    }

    /** Returns whether a resource is eligible under the supplied configuration. */
    public static boolean isModTexture(String namespace, String path, MtsrConfig config) {
        if (namespace == null || path == null) {
            return false;
        }
        if (EXCLUDED_NAMESPACES.contains(namespace)) {
            return false;
        }
        if (config.extraExcludedNamespaces().contains(namespace)) {
            return false;
        }
        String resourcePath = namespace + ":" + path;
        if (config.forceIncludedPaths().stream().anyMatch(resourcePath::startsWith)) {
            return path.endsWith(".png");
        }
        return path.startsWith("textures/") && path.endsWith(".png");
    }

    /** Returns all built-in and configured excluded namespaces. */
    public static Set<String> excludedNamespaces(MtsrConfig config) {
        Set<String> excluded = new LinkedHashSet<>();
        excluded.add("minecraft");
        excluded.add("realms");
        excluded.add("mtsr");
        config.extraExcludedNamespaces().stream().sorted().forEach(excluded::add);
        return Collections.unmodifiableSet(excluded);
    }
}
