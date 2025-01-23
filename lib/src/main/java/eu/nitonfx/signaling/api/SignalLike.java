package eu.nitonfx.signaling.api;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A signal is a value that can be subscribed to
 * @param <T> the type of the value
 */
public interface SignalLike<T> extends Supplier<T> {
    T get();
    T getUntracked();
    StackTraceElement getOrigin();

    /**
     * @param consumer listener called when the signal was recomputed and has a new changed value, this call doesn't need to trigger a {@link #onDirty(Consumer)}
     * @return an object that can be used to cancel the subscription
     */
    Subscription onChange(Consumer<T> consumer);

    /**
     * @param consumer listener that is called when the signal might need to be read again because the value might have changed,
     *                 it is NOT necessary that this is called when {@link #onChange(Consumer)} trigger/ed/s
     * @return an object that can be used to cancel the subscription
     */
    Subscription onDirty(Consumer<SignalLike<T>> consumer);
}
