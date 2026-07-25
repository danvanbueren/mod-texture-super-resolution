package me.danvb10.mtsr.upscale.detect;

import me.danvb10.mtsr.config.MtsrConfig;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpriteUpscalePolicyTest {

    @Test
    void toTexturePathMapsSpritePath() {
        assertEquals("textures/block/machine.png",
                SpriteUpscalePolicy.toTexturePath("block/machine"));
        assertEquals("textures/item/wrench.png",
                SpriteUpscalePolicy.toTexturePath("item/wrench"));
    }

    @Test
    void modNamespaceSpritesAreEligible() {
        assertTrue(SpriteUpscalePolicy.isEligibleSprite("somemod", "block/machine"));
        assertTrue(SpriteUpscalePolicy.isEligibleSprite("somemod", "item/wrench"));
    }

    @Test
    void excludedNamespacesAreNotEligible() {
        assertFalse(SpriteUpscalePolicy.isEligibleSprite("minecraft", "block/stone"));
        assertFalse(SpriteUpscalePolicy.isEligibleSprite("realms", "block/stone"));
        assertFalse(SpriteUpscalePolicy.isEligibleSprite("mtsr", "icon"));
    }

    @Test
    void nullInputsAreNotEligible() {
        assertFalse(SpriteUpscalePolicy.isEligibleSprite(null, "block/stone"));
        assertFalse(SpriteUpscalePolicy.isEligibleSprite("somemod", null));
    }

    @Test
    void configuredForcePathMakesSpriteEligible() {
        MtsrConfig config = MtsrConfig.defaults();
        config.forceIncludedPaths(Set.of("somemod:textures/block/"));

        assertTrue(SpriteUpscalePolicy.isEligibleSprite("somemod", "block/machine", config));
    }

    @Test
    void validUpscaleRequiresExactScaledDimensions() {
        assertTrue(SpriteUpscalePolicy.isValidUpscale(16, 16, 64, 64, 4));
        assertTrue(SpriteUpscalePolicy.isValidUpscale(16, 32, 32, 64, 2));
        assertFalse(SpriteUpscalePolicy.isValidUpscale(16, 16, 64, 32, 4));
        assertFalse(SpriteUpscalePolicy.isValidUpscale(16, 16, 32, 32, 4));
    }

    @Test
    void invalidDimensionsOrScaleAreRejected() {
        assertFalse(SpriteUpscalePolicy.isValidUpscale(0, 16, 0, 64, 4));
        assertFalse(SpriteUpscalePolicy.isValidUpscale(16, 0, 64, 0, 4));
        assertFalse(SpriteUpscalePolicy.isValidUpscale(16, 16, 0, 0, 0));
    }
}
