package me.danvb10.mtsr.upscale.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Downloads a default Real-ESRGAN ONNX model into the mod's model directory
 * ({@code config/mtsr/models}) on first launch, so texture upscaling works
 * out of the box without manual setup.
 *
 * <p>The default model is {@code realesr-general-x4v3} (4x general-purpose
 * upscaler, ~5 MB) from the <a href="https://github.com/xinntao/Real-ESRGAN">
 * Real-ESRGAN</a> project by Xintao Wang et al., licensed under the
 * BSD 3-Clause License. It is fetched from a revision-pinned Hugging Face
 * mirror and verified against a hardcoded SHA-256 checksum before being
 * moved into place.</p>
 *
 * <p>The download only triggers when the model directory contains no
 * {@code .onnx} file, runs on a background daemon thread so the game thread
 * is never blocked, and makes at most one attempt per launch.</p>
 */
public final class ModelDownloader {

    /** Revision-pinned URL of the default model (BSD-3-Clause licensed). */
    public static final String DEFAULT_MODEL_URL =
            "https://huggingface.co/Heliosoph/realesrgan-onnx/resolve/"
                    + "488e5dda07333179f229a6205d92135eea4c25e9/realesr-general-x4v3.onnx";

    /** Expected SHA-256 of the default model file. */
    public static final String DEFAULT_MODEL_SHA256 =
            "09b757accd747d7e423c1d352b3e8f23e77cc5742d04bae958d4eb8082b76fa4";

    /** File name the default model is saved as; the -x4 suffix marks it as 4x. */
    public static final String DEFAULT_MODEL_FILE_NAME = "realesr-general-x4.onnx";

    private static final Logger LOGGER = LoggerFactory.getLogger("mtsr");
    private static final int PROGRESS_LOG_STEP_BYTES = 1024 * 1024;

    private final Path modelDirectory;
    private final URI sourceUri;
    private final String expectedSha256;
    private final String fileName;
    private final AtomicBoolean attempted = new AtomicBoolean();

    public ModelDownloader(Path modelDirectory) {
        this(modelDirectory, URI.create(DEFAULT_MODEL_URL),
                DEFAULT_MODEL_SHA256, DEFAULT_MODEL_FILE_NAME);
    }

    public ModelDownloader(Path modelDirectory, URI sourceUri,
                           String expectedSha256, String fileName) {
        this.modelDirectory = modelDirectory;
        this.sourceUri = sourceUri;
        this.expectedSha256 = expectedSha256;
        this.fileName = fileName;
    }

    /**
     * Starts a background download of the default model if the model
     * directory contains no .onnx file yet. At most one attempt is made
     * per instance; failures are logged and left for the next launch.
     */
    public void downloadIfMissing() {
        if (!attempted.compareAndSet(false, true)) {
            return;
        }
        if (hasModel()) {
            return;
        }
        Thread thread = new Thread(this::runDownload, "mtsr-model-downloader");
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    /** Whether the model directory already contains an .onnx file. */
    public boolean hasModel() {
        if (!Files.isDirectory(modelDirectory)) {
            return false;
        }
        try (var files = Files.list(modelDirectory)) {
            return files.anyMatch(p -> p.getFileName().toString()
                    .toLowerCase(java.util.Locale.ROOT).endsWith(".onnx"));
        } catch (IOException e) {
            LOGGER.warn("Failed to inspect model directory {}", modelDirectory, e);
            return false;
        }
    }

    private void runDownload() {
        LOGGER.info("No ESRGAN model found; downloading default Real-ESRGAN model "
                + "(realesr-general-x4v3, BSD-3-Clause) from {}", sourceUri);
        try {
            Path target = download();
            LOGGER.info("Default ESRGAN model installed at {}. It will be used "
                    + "the next time textures are (re)loaded.", target);
        } catch (IOException | ChecksumMismatchException e) {
            LOGGER.error("Failed to download default ESRGAN model from {}. "
                    + "Will not retry this launch; you can place a Real-ESRGAN "
                    + ".onnx model in {} manually.", sourceUri, modelDirectory, e);
        }
    }

    /**
     * Downloads the model to a .tmp file in the model directory, verifies its
     * SHA-256 checksum, and atomically moves it to its final name.
     *
     * @return the path of the installed model file
     */
    public Path download() throws IOException, ChecksumMismatchException {
        Files.createDirectories(modelDirectory);
        Path target = modelDirectory.resolve(fileName);
        Path temp = modelDirectory.resolve(fileName + ".tmp");
        MessageDigest digest = sha256Digest();
        URLConnection connection = sourceUri.toURL().openConnection();
        connection.setConnectTimeout(15_000);
        connection.setReadTimeout(60_000);
        long totalBytes = connection.getContentLengthLong();
        try {
            try (InputStream in = new DigestInputStream(connection.getInputStream(), digest)) {
                copyWithProgress(in, temp, totalBytes);
            }
            String actual = HexFormat.of().formatHex(digest.digest());
            if (!actual.equalsIgnoreCase(expectedSha256)) {
                throw new ChecksumMismatchException(
                        "SHA-256 mismatch for downloaded model: expected "
                                + expectedSha256 + " but got " + actual);
            }
            move(temp, target);
            return target;
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private void copyWithProgress(InputStream in, Path temp, long totalBytes)
            throws IOException {
        byte[] buffer = new byte[64 * 1024];
        long copied = 0;
        long nextLog = PROGRESS_LOG_STEP_BYTES;
        try (var out = Files.newOutputStream(temp)) {
            int read;
            while ((read = in.read(buffer)) >= 0) {
                out.write(buffer, 0, read);
                copied += read;
                if (copied >= nextLog) {
                    nextLog += PROGRESS_LOG_STEP_BYTES;
                    if (totalBytes > 0) {
                        LOGGER.info("Model download progress: {} / {} MiB",
                                copied / (1024 * 1024), (totalBytes + 1024 * 1024 - 1) / (1024 * 1024));
                    } else {
                        LOGGER.info("Model download progress: {} MiB", copied / (1024 * 1024));
                    }
                }
            }
        }
    }

    private static void move(Path temp, Path target) throws IOException {
        try {
            Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static MessageDigest sha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    /** Thrown when a downloaded file's checksum does not match the expected value. */
    public static final class ChecksumMismatchException extends Exception {
        public ChecksumMismatchException(String message) {
            super(message);
        }
    }
}
