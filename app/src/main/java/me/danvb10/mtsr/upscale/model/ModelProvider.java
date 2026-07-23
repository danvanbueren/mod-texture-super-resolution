package me.danvb10.mtsr.upscale.model;

import java.util.Optional;

/** Supplies the currently active upscale model, if any. */
@FunctionalInterface
public interface ModelProvider {

    Optional<UpscaleModel> activeModel();
}
