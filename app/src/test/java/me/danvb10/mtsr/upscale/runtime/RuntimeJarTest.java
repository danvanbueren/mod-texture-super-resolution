package me.danvb10.mtsr.upscale.runtime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeJarTest {

    // SHA-256 of the ASCII string "hello"
    private static final String HELLO_SHA256 =
            "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824";

    private static final RuntimeJar JAR = new RuntimeJar(
            "com.example.group", "artifact", "1.2.3", HELLO_SHA256.toUpperCase());

    @TempDir
    Path tempDir;

    @Test
    void buildsFileName() {
        assertEquals("artifact-1.2.3.jar", JAR.fileName());
    }

    @Test
    void buildsMavenCentralUrl() {
        assertEquals("https://repo1.maven.org/maven2/com/example/group/artifact/1.2.3/artifact-1.2.3.jar",
                JAR.downloadUrl());
    }

    @Test
    void normalizesChecksumToLowercase() {
        assertEquals(HELLO_SHA256, JAR.sha256());
    }

    @Test
    void resolvesPathInsideDirectory() {
        assertEquals(tempDir.resolve("artifact-1.2.3.jar"), JAR.resolve(tempDir));
    }

    @Test
    void onnxRuntimeSpecPinsExactVersion() {
        RuntimeJar ort = RuntimeJar.ONNXRUNTIME;
        assertEquals("onnxruntime-1.20.0.jar", ort.fileName());
        assertEquals("https://repo1.maven.org/maven2/com/microsoft/onnxruntime/onnxruntime/1.20.0/onnxruntime-1.20.0.jar",
                ort.downloadUrl());
        assertEquals(64, ort.sha256().length());
    }

    @Test
    void computesSha256OfFileAndBytes() throws IOException {
        Path file = tempDir.resolve("hello.txt");
        Files.writeString(file, "hello");
        assertEquals(HELLO_SHA256, RuntimeJar.sha256Hex(file));
        assertEquals(HELLO_SHA256, RuntimeJar.sha256Hex("hello".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void isValidAcceptsMatchingFile() throws IOException {
        Path file = JAR.resolve(tempDir);
        Files.writeString(file, "hello");
        assertTrue(JAR.isValid(file));
    }

    @Test
    void isValidRejectsMissingOrCorruptFile() throws IOException {
        Path file = JAR.resolve(tempDir);
        assertFalse(JAR.isValid(file), "missing file");
        Files.writeString(file, "tampered");
        assertFalse(JAR.isValid(file), "checksum mismatch");
    }
}
