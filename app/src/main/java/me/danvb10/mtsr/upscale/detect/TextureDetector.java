package me.danvb10.mtsr.upscale.detect;

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
        if (namespace == null || path == null) {
            return false;
        }
        if (EXCLUDED_NAMESPACES.contains(namespace)) {
            return false;
        }
        return path.startsWith("textures/") && path.endsWith(".png");
    }
}
