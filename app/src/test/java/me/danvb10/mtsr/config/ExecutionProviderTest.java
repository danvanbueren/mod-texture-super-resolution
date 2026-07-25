package me.danvb10.mtsr.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExecutionProviderTest {

    @Test
    void supportsConfiguredProviderValues() {
        assertEquals(ExecutionProvider.CPU, ExecutionProvider.valueOf("CPU"));
        assertEquals(ExecutionProvider.DIRECTML, ExecutionProvider.valueOf("DIRECTML"));
        assertEquals(ExecutionProvider.CUDA, ExecutionProvider.valueOf("CUDA"));
    }
}
