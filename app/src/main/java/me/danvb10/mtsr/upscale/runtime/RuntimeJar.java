package me.danvb10.mtsr.upscale.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

/**
 * A runtime-downloaded Maven dependency, pinned to an exact version and
 * SHA-256 checksum. Pure value type so the URL / path / checksum logic is
 * unit-testable without any network access.
 */
public record RuntimeJar(String groupId, String artifactId, String version, String sha256) {

    private static final String MAVEN_CENTRAL = "https://repo1.maven.org/maven2";

    /** ONNX Runtime, downloaded on first use instead of being bundled in the mod jar. */
    public static final RuntimeJar ONNXRUNTIME = new RuntimeJar(
            "com.microsoft.onnxruntime", "onnxruntime", "1.20.0",
            "c46608681692b3693914defb42fa3119ad7cb6146870581ece247e0ed5793fe8");

    public RuntimeJar {
        sha256 = sha256.toLowerCase(Locale.ROOT);
    }

    /** File name of the jar, e.g. {@code onnxruntime-1.20.0.jar}. */
    public String fileName() {
        return artifactId + "-" + version + ".jar";
    }

    /** Maven Central download URL for the jar. */
    public String downloadUrl() {
        return MAVEN_CENTRAL + "/" + groupId.replace('.', '/') + "/" + artifactId
                + "/" + version + "/" + fileName();
    }

    /** Resolves the on-disk location of the jar inside the given directory. */
    public Path resolve(Path directory) {
        return directory.resolve(fileName());
    }

    /** Whether the file exists and matches the pinned checksum. */
    public boolean isValid(Path file) throws IOException {
        return Files.isRegularFile(file) && sha256.equals(sha256Hex(file));
    }

    /** Computes the lowercase hex SHA-256 digest of a file. */
    public static String sha256Hex(Path file) throws IOException {
        MessageDigest digest = newSha256();
        try (InputStream in = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    /** Computes the lowercase hex SHA-256 digest of a byte array. */
    public static String sha256Hex(byte[] bytes) {
        return HexFormat.of().formatHex(newSha256().digest(bytes));
    }

    private static MessageDigest newSha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
