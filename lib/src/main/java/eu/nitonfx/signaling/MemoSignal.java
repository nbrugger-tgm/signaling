package eu.nitonfx.signaling;

import eu.nitonfx.signaling.api.Context;
import eu.nitonfx.signaling.api.Signal;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.function.Supplier;

public class MemoSignal<T> implements Supplier<T> {
    private final Signal<T> cache;

    public MemoSignal(Context cx, Supplier<T> function, T initial) {
        cache = cx.createSignal(initial);
        cx.createEffect(()-> cache.set(function.get()));
    }

    @Override
    public T get() {
        return cache.get();
    }
}
