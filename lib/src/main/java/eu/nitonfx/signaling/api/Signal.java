package eu.nitonfx.signaling.api;

import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * A signal is a value that can be subscribed to and updated.
 *
 * @param <T> the type of the value
 */
public interface Signal<T> extends SignalLike<T>, Consumer<T> {

    /**
     * Sets the value of the signal.
     * This method updates the value of the signal and notifies subscribers of the change.
     *
     * @param i the new value of the signal
     */
    void set(T i);

    @Override
    default void accept(T t) {
        set(t);
    }

    /**
     * Updates the value of the signal using the given operator.
     * This method applies the operator to the current value of the signal and sets the result as the new value.
     *
     * @param o the operator to apply to the current value
     */
    default void update(UnaryOperator<T> o){
        set(o.apply(getUntracked()));
    }
}
