package me.danvb10.mtsr.upscale.detect;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DetectedTextureRegistryTest {

    private DetectedTextureRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new DetectedTextureRegistry();
    }

    @Test
    void registerAndRetrieveTextureByNamespace() {
        byte[] dummyPng = new byte[]{1, 2, 3, 4};
        registry.registerOriginal("create:block/cogwheel", "create", "block/cogwheel", dummyPng, 16, 16);
        registry.registerOriginal("botania:item/lexicon", "botania", "item/lexicon", dummyPng, 16, 16);

        Set<String> namespaces = registry.getNamespaces();
        assertEquals(Set.of("botania", "create"), namespaces);

        List<DetectedTexture> createTextures = registry.getTexturesByNamespace("create");
        assertEquals(1, createTextures.size());
        assertEquals("create:block/cogwheel", createTextures.get(0).textureId());

        assertEquals(2, registry.totalCount());
    }

    @Test
    void registerUpscaledUpdatesStatusAndDimensions() {
        byte[] origPng = new byte[]{1, 2, 3};
        byte[] upPng = new byte[]{4, 5, 6, 7};

        registry.registerOriginal("create:block/cogwheel", "create", "block/cogwheel", origPng, 16, 16);
        registry.registerUpscaled("create:block/cogwheel", upPng, 64, 64, DetectedTexture.Status.UPSCALED);

        DetectedTexture texture = registry.findTexture("create:block/cogwheel");
        assertNotNull(texture);
        assertEquals(64, texture.upscaledWidth());
        assertEquals(64, texture.upscaledHeight());
        assertEquals(DetectedTexture.Status.UPSCALED, texture.status());
        assertEquals(1, registry.upscaledCount());
    }

    @Test
    void namespaceDisabledOverridesAllTexturesInNamespace() {
        byte[] dummyPng = new byte[]{1, 2};
        registry.registerOriginal("create:block/cog1", "create", "block/cog1", dummyPng, 16, 16);
        registry.registerOriginal("create:block/cog2", "create", "block/cog2", dummyPng, 16, 16);

        registry.setNamespaceDisabled("create", true);

        assertTrue(registry.findTexture("create:block/cog1").isDisabled());
        assertTrue(registry.findTexture("create:block/cog2").isDisabled());
        assertEquals(2, registry.disabledCount());

        registry.setNamespaceDisabled("create", false);
        assertFalse(registry.findTexture("create:block/cog1").isDisabled());
    }

    @Test
    void tagForRegenerationTracksBadUpscales() {
        byte[] dummyPng = new byte[]{1, 2};
        registry.registerOriginal("ae2:item/press", "ae2", "item/press", dummyPng, 16, 16);

        assertFalse(registry.findTexture("ae2:item/press").isTaggedForRegen());
        assertEquals(0, registry.taggedCount());

        registry.setTextureTaggedForRegen("ae2:item/press", true);

        assertTrue(registry.findTexture("ae2:item/press").isTaggedForRegen());
        assertEquals(1, registry.taggedCount());
        assertEquals(1, registry.getTaggedTextures().size());
        assertEquals("ae2:item/press", registry.getTaggedTextures().get(0).textureId());

        registry.setTextureTaggedForRegen("ae2:item/press", false);
        assertEquals(0, registry.taggedCount());
    }
}
