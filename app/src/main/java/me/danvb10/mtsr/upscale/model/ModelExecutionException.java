package me.danvb10.mtsr.upscale.model;

/** Thrown when a model fails to load or run inference. */
public class ModelExecutionException extends Exception {

    public ModelExecutionException(String message) {
        super(message);
    }

    public ModelExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
