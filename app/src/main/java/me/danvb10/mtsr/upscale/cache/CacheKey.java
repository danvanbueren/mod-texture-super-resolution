package me.danvb10.mtsr.upscale.cache;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Content-addressed cache key: SHA-256 over the original texture bytes plus
 * the model identity, so a cache entry is invalidated whenever the source
 * texture, the model, or its scale changes.
 */
public record CacheKey(String hash) {

    public static CacheKey of(byte[] textureBytes, String modelName, int scaleFactor) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(textureBytes);
            digest.update((byte) 0);
            digest.update(modelName.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            digest.update(Integer.toString(scaleFactor).getBytes(StandardCharsets.UTF_8));
            return new CacheKey(toHex(digest.digest()));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    /** File name of this entry inside the cache directory. */
    public String fileName() {
        return hash + ".png";
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }
}
