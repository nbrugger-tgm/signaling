package eu.nitonfx.signaling.api;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public interface Signal<T> extends Supplier<T>, Consumer<T> {
    void set(T i);

    T get();

    @Override
    default void accept(T t) {
        set(t);
    }

    default <O> Supplier<O> derive(Function<T, O> derivation) {
        return () -> derivation.apply(get());
    }
}
