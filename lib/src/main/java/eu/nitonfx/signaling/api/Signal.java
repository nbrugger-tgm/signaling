package eu.nitonfx.signaling.api;

import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * A signal is a value that can be subscribed to
 * @param <T> the type of the value
 */
public interface Signal<T> extends SignalLike<T>, Consumer<T> {
    void set(T i);

    @Override
    default void accept(T t) {
        set(t);
    }

    default void update(UnaryOperator<T> o){
        set(o.apply(getUntracked()));
    }
}
