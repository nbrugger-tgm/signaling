package eu.nitonfx.signaling.api;

/**
 * Represents a handle for an effect that allows manual cancellation.
 * The cancel method can be used to stop the effect and clean up any resources associated with it.
 */
public interface EffectHandle {
    void cancel();
}
