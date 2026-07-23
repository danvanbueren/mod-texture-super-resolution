package me.danvb10.mtsr.upscale.model;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;

import java.nio.FloatBuffer;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * ESRGAN super-resolution model backed by an ONNX Runtime session.
 *
 * <p>Expects a Real-ESRGAN style graph: single input of shape [1, 3, H, W]
 * with RGB values normalized to 0..1, single output of shape
 * [1, 3, H * scale, W * scale]. Large images are processed in overlapping
 * tiles to bound memory usage; the alpha channel is upscaled separately with
 * nearest-neighbor sampling to preserve cutout transparency.</p>
 */
public final class EsrganModel implements UpscaleModel {

    private final String name;
    private final int scaleFactor;
    private final int tileSize;
    private final int tileOverlap;
    private final OrtEnvironment environment;
    private final OrtSession session;
    private final String inputName;

    private EsrganModel(String name, int scaleFactor, int tileSize, int tileOverlap,
                        OrtEnvironment environment, OrtSession session, String inputName) {
        this.name = name;
        this.scaleFactor = scaleFactor;
        this.tileSize = tileSize;
        this.tileOverlap = tileOverlap;
        this.environment = environment;
        this.session = session;
        this.inputName = inputName;
    }

    /** Loads an ESRGAN ONNX model from disk. */
    public static EsrganModel load(Path modelFile, String name, int scaleFactor,
                                   int tileSize, int tileOverlap) throws ModelExecutionException {
        try {
            OrtEnvironment environment = OrtEnvironment.getEnvironment();
            OrtSession.SessionOptions options = new OrtSession.SessionOptions();
            options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);
            OrtSession session = environment.createSession(modelFile.toString(), options);
            String inputName = session.getInputNames().iterator().next();
            return new EsrganModel(name, scaleFactor, tileSize, tileOverlap,
                    environment, session, inputName);
        } catch (OrtException e) {
            throw new ModelExecutionException("Failed to load ONNX model " + modelFile, e);
        }
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public int scaleFactor() {
        return scaleFactor;
    }

    @Override
    public int[] upscale(int[] argb, int width, int height) throws ModelExecutionException {
        int[] result = new int[width * scaleFactor * height * scaleFactor];
        if (width <= tileSize && height <= tileSize) {
            int[] upscaled = runInference(argb, width, height);
            System.arraycopy(upscaled, 0, result, 0, upscaled.length);
        } else {
            List<Tiling.Tile> tiles = Tiling.computeTiles(width, height, tileSize, tileOverlap);
            for (Tiling.Tile tile : tiles) {
                int[] tilePixels = Tiling.extract(argb, width, tile);
                int[] upscaledTile = runInference(tilePixels, tile.width(), tile.height());
                Tiling.stitch(upscaledTile, tile, scaleFactor, result, width * scaleFactor);
            }
        }
        ImageOps.upscaleAlphaNearest(argb, width, height, result, scaleFactor);
        return result;
    }

    private int[] runInference(int[] argb, int width, int height) throws ModelExecutionException {
        float[] input = ImageOps.argbToChwRgb(argb, width, height);
        long[] shape = {1, 3, height, width};
        try (OnnxTensor tensor = OnnxTensor.createTensor(environment, FloatBuffer.wrap(input), shape);
             OrtSession.Result outputs = session.run(Map.of(inputName, tensor))) {
            OnnxTensor output = (OnnxTensor) outputs.get(0);
            FloatBuffer buffer = output.getFloatBuffer();
            float[] chw = new float[buffer.remaining()];
            buffer.get(chw);
            int outWidth = width * scaleFactor;
            int outHeight = height * scaleFactor;
            if (chw.length != 3 * outWidth * outHeight) {
                throw new ModelExecutionException("Unexpected output size " + chw.length
                        + " for " + outWidth + "x" + outHeight + " (model scale mismatch?)");
            }
            return ImageOps.chwRgbToArgb(chw, outWidth, outHeight);
        } catch (OrtException e) {
            throw new ModelExecutionException("Inference failed for " + width + "x" + height, e);
        }
    }

    @Override
    public void close() {
        try {
            session.close();
        } catch (Exception ignored) {
        }
    }
}
