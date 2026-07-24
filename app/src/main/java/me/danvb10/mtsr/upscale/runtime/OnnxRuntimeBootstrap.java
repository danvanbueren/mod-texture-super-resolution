package me.danvb10.mtsr.upscale.runtime;

import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

/**
 * Makes the ONNX Runtime classes available at runtime without bundling the
 * (~90 MB) onnxruntime jar inside the mod. On first use the jar is downloaded
 * from Maven Central into {@code config/mtsr/runtime/}, verified against a
 * pinned SHA-256 checksum, and appended to the game classpath before any
 * {@code ai.onnxruntime} class is touched.
 *
 * <p>At most one download is attempted per game launch; callers must invoke
 * this from a background thread, never from the game/render thread.</p>
 */
public final class OnnxRuntimeBootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger("mtsr");
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(30);

    private static boolean attempted;
    private static boolean available;

    private OnnxRuntimeBootstrap() {
    }

    /**
     * Ensures ONNX Runtime is on the classpath, downloading it if necessary.
     * Only the first call does any work; subsequent calls return the cached
     * result. Returns {@code true} if {@code ai.onnxruntime} classes are
     * usable.
     */
    public static synchronized boolean ensureAvailable(Path runtimeDirectory) {
        if (attempted) {
            return available;
        }
        attempted = true;
        available = bootstrap(runtimeDirectory);
        return available;
    }

    private static boolean bootstrap(Path runtimeDirectory) {
        if (isOnnxRuntimePresent()) {
            LOGGER.info("ONNX Runtime already present on the classpath");
            return true;
        }
        RuntimeJar jar = RuntimeJar.ONNXRUNTIME;
        Path jarFile = jar.resolve(runtimeDirectory);
        try {
            if (!jar.isValid(jarFile)) {
                download(jar, jarFile);
            } else {
                LOGGER.info("Using cached ONNX Runtime jar at {}", jarFile);
            }
        } catch (IOException | InterruptedException e) {
            LOGGER.error("Failed to download ONNX Runtime {} from {}. Texture upscaling is "
                    + "disabled for this session; it will be retried next launch.",
                    jar.version(), jar.downloadUrl(), e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
        return addToClasspath(jarFile);
    }

    private static boolean isOnnxRuntimePresent() {
        try {
            Class.forName("ai.onnxruntime.OrtEnvironment", false,
                    OnnxRuntimeBootstrap.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static void download(RuntimeJar jar, Path jarFile)
            throws IOException, InterruptedException {
        LOGGER.info("Downloading ONNX Runtime {} (~90 MB, one-time) from {}",
                jar.version(), jar.downloadUrl());
        Files.createDirectories(jarFile.getParent());
        Path partFile = jarFile.resolveSibling(jarFile.getFileName() + ".part");
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(jar.downloadUrl())).GET().build();
        HttpResponse<InputStream> response =
                client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            throw new IOException("HTTP " + response.statusCode() + " from " + jar.downloadUrl());
        }
        try (InputStream body = response.body()) {
            Files.copy(body, partFile, StandardCopyOption.REPLACE_EXISTING);
        }
        String actual = RuntimeJar.sha256Hex(partFile);
        if (!jar.sha256().equals(actual)) {
            Files.deleteIfExists(partFile);
            throw new IOException("Checksum mismatch for " + jar.fileName()
                    + ": expected " + jar.sha256() + " but was " + actual);
        }
        Files.move(partFile, jarFile, StandardCopyOption.REPLACE_EXISTING);
        LOGGER.info("Downloaded and verified ONNX Runtime jar at {}", jarFile);
    }

    private static boolean addToClasspath(Path jarFile) {
        try {
            FabricLauncherBase.getLauncher().addToClassPath(jarFile);
        } catch (Throwable t) {
            LOGGER.error("Failed to add {} to the game classpath; texture upscaling is disabled",
                    jarFile, t);
            return false;
        }
        if (isOnnxRuntimePresent()) {
            LOGGER.info("ONNX Runtime loaded from {}", jarFile);
            return true;
        }
        LOGGER.error("ONNX Runtime classes still unavailable after adding {} to the classpath; "
                + "texture upscaling is disabled", jarFile);
        return false;
    }
}
