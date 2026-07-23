package me.danvb10.mtsr.upscale.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImageOpsTest {

    @Test
    void chwRoundTripPreservesRgbAndForcesOpaqueAlpha() {
        int[] argb = {0x80FF0000, 0xFF00FF00, 0x000000FF, 0xFF123456};
        float[] chw = ImageOps.argbToChwRgb(argb, 2, 2);
        int[] back = ImageOps.chwRgbToArgb(chw, 2, 2);
        assertEquals(0xFFFF0000, back[0]);
        assertEquals(0xFF00FF00, back[1]);
        assertEquals(0xFF0000FF, back[2]);
        assertEquals(0xFF123456, back[3]);
    }

    @Test
    void chwLayoutIsChannelMajor() {
        int[] argb = {0xFFFF0000, 0xFF00FF00};
        float[] chw = ImageOps.argbToChwRgb(argb, 2, 1);
        assertEquals(1.0f, chw[0]); // R of pixel 0
        assertEquals(0.0f, chw[1]); // R of pixel 1
        assertEquals(0.0f, chw[2]); // G of pixel 0
        assertEquals(1.0f, chw[3]); // G of pixel 1
        assertEquals(0.0f, chw[4]); // B of pixel 0
        assertEquals(0.0f, chw[5]); // B of pixel 1
    }

    @Test
    void clampsOutOfRangeModelOutput() {
        float[] chw = {1.5f, -0.5f, 0.5f};
        int[] argb = ImageOps.chwRgbToArgb(chw, 1, 1);
        assertEquals(0xFFFF0080, argb[0]);
    }

    @Test
    void alphaUpscaleUsesNearestNeighborSampling() {
        int[] source = {0x00FFFFFF, 0xFFFFFFFF, 0x80FFFFFF, 0x40FFFFFF};
        int scale = 2;
        int[] target = new int[16];
        Arrays.fill(target, 0xFF102030);
        ImageOps.upscaleAlphaNearest(source, 2, 2, target, scale);
        assertEquals(0x00102030, target[0]);
        assertEquals(0x00102030, target[1]);
        assertEquals(0xFF102030, target[2]);
        assertEquals(0xFF102030, target[3]);
        assertEquals(0x80102030, target[8]);
        assertEquals(0x40102030, target[10]);
    }
}
