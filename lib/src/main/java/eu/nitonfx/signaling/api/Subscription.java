package eu.nitonfx.signaling.api;

/**
 * Represents a subscription to a signal or effect.
 * Provides a method to unsubscribe from the signal or effect.
 */
public interface Subscription {

    /**
     * Unsubscribes from the signal or effect.
     * This method should be called to stop receiving updates from the signal or effect.
     */
    void unsubscribe();
}
