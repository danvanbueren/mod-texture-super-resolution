package me.danvb10.mtsr.upscale.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelDownloaderTest {

    @TempDir
    Path sourceDir;

    @TempDir
    Path modelDir;

    private static final byte[] PAYLOAD = "fake onnx model bytes".getBytes();

    private URI localSource() throws IOException {
        Path source = sourceDir.resolve("model.onnx");
        Files.write(source, PAYLOAD);
        return source.toUri();
    }

    private static String sha256(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void downloadsVerifiesAndInstallsModel() throws Exception {
        ModelDownloader downloader = new ModelDownloader(
                modelDir, localSource(), sha256(PAYLOAD), "default-x4.onnx");
        Path installed = downloader.download();
        assertEquals(modelDir.resolve("default-x4.onnx"), installed);
        assertArrayEquals(PAYLOAD, Files.readAllBytes(installed));
        assertFalse(Files.exists(modelDir.resolve("default-x4.onnx.tmp")));
    }

    @Test
    void acceptsUppercaseExpectedChecksum() throws Exception {
        ModelDownloader downloader = new ModelDownloader(
                modelDir, localSource(), sha256(PAYLOAD).toUpperCase(), "default-x4.onnx");
        assertTrue(Files.exists(downloader.download()));
    }

    @Test
    void rejectsChecksumMismatchAndLeavesNoFiles() throws Exception {
        ModelDownloader downloader = new ModelDownloader(
                modelDir, localSource(), "00".repeat(32), "default-x4.onnx");
        assertThrows(ModelDownloader.ChecksumMismatchException.class, downloader::download);
        assertFalse(Files.exists(modelDir.resolve("default-x4.onnx")));
        assertFalse(Files.exists(modelDir.resolve("default-x4.onnx.tmp")));
    }

    @Test
    void createsModelDirectoryIfMissing() throws Exception {
        Path nested = modelDir.resolve("config/mtsr/models");
        ModelDownloader downloader = new ModelDownloader(
                nested, localSource(), sha256(PAYLOAD), "default-x4.onnx");
        Path installed = downloader.download();
        assertTrue(Files.exists(installed));
        assertEquals(nested, installed.getParent());
    }

    @Test
    void hasModelDetectsOnnxFilesOnly() throws IOException {
        ModelDownloader downloader = new ModelDownloader(
                modelDir, URI.create("file:///nonexistent"), "00", "default-x4.onnx");
        assertFalse(downloader.hasModel());
        Files.writeString(modelDir.resolve("readme.txt"), "");
        assertFalse(downloader.hasModel());
        Files.writeString(modelDir.resolve("Existing-X4.ONNX"), "");
        assertTrue(downloader.hasModel());
    }

    @Test
    void downloadIfMissingSkipsWhenModelPresent() throws Exception {
        Files.writeString(modelDir.resolve("existing.onnx"), "");
        ModelDownloader downloader = new ModelDownloader(
                modelDir, localSource(), sha256(PAYLOAD), "default-x4.onnx");
        downloader.downloadIfMissing();
        Thread.sleep(200);
        assertFalse(Files.exists(modelDir.resolve("default-x4.onnx")));
    }

    @Test
    void defaultFileNameParsesAsFourTimesScale() {
        String name = ModelDownloader.DEFAULT_MODEL_FILE_NAME;
        assertTrue(name.endsWith(".onnx"));
        String base = name.substring(0, name.length() - ".onnx".length());
        assertEquals(4, ModelManager.parseScaleFromName(base));
    }
}
