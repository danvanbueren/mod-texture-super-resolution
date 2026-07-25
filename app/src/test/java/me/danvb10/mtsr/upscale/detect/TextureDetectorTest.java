package me.danvb10.mtsr.upscale.detect;

import me.danvb10.mtsr.config.MtsrConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextureDetectorTest {

    @Test
    void acceptsModTextures() {
        assertTrue(TextureDetector.isModTexture("somemod", "textures/item/gadget.png"));
        assertTrue(TextureDetector.isModTexture("othermod", "textures/block/ore.png"));
    }

    @Test
    void rejectsVanillaAndOwnNamespaces() {
        assertFalse(TextureDetector.isModTexture("minecraft", "textures/block/stone.png"));
        assertFalse(TextureDetector.isModTexture("realms", "textures/gui/title.png"));
        assertFalse(TextureDetector.isModTexture("mtsr", "textures/gui/icon.png"));
    }

    @Test
    void rejectsNonTexturePaths() {
        assertFalse(TextureDetector.isModTexture("somemod", "models/item/gadget.json"));
        assertFalse(TextureDetector.isModTexture("somemod", "textures/item/gadget.png.mcmeta"));
        assertFalse(TextureDetector.isModTexture("somemod", "sounds/gadget.png"));
    }

    @Test
    void rejectsNullInputs() {
        assertFalse(TextureDetector.isModTexture(null, "textures/a.png"));
        assertFalse(TextureDetector.isModTexture("somemod", null));
    }

    @Test
    void configuredNamespacesAreExcluded() {
        MtsrConfig config = MtsrConfig.defaults();
        config.extraExcludedNamespaces(java.util.Set.of("optifine"));

        assertFalse(TextureDetector.isModTexture("optifine", "textures/item/a.png", config));
        assertTrue(TextureDetector.isModTexture("somemod", "textures/item/a.png", config));
    }

    @Test
    void forceIncludedPathsOverridePathAndExtension() {
        MtsrConfig config = MtsrConfig.defaults();
        config.forceIncludedPaths(java.util.Set.of("somemod:textures/custom/"));

        assertTrue(TextureDetector.isModTexture("somemod", "textures/custom/data.json", config));
        assertFalse(TextureDetector.isModTexture("minecraft", "textures/custom/data.json", config));
    }
}
