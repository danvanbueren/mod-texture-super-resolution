package me.danvb10.mtsr.upscale;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnimatedFrameLayoutTest {

    @Test
    void describesWholeVerticalAndGridFrameLayouts() {
        AnimatedFrameLayout strip = AnimatedFrameLayout.of(16, 32, 16, 8);
        assertEquals(4, strip.frameCount());
        assertEquals(32, strip.outputWidth(2));
        assertEquals(64, strip.outputHeight(2));

        AnimatedFrameLayout grid = AnimatedFrameLayout.of(32, 32, 16, 16);
        assertEquals(4, grid.frameCount());
    }

    @Test
    void rejectsNonIntegralFrameDivisions() {
        assertThrows(IllegalArgumentException.class,
                () -> AnimatedFrameLayout.of(17, 32, 16, 16));
        assertThrows(IllegalArgumentException.class,
                () -> AnimatedFrameLayout.of(32, 31, 16, 16));
    }
}
