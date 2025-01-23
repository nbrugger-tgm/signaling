package eu.nitonfx.signaling;

import eu.nitonfx.signaling.api.SignalLike;
import eu.nitonfx.signaling.api.Subscription;
import eu.nitonfx.signaling.api.Signal;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * A signal that holds a value that can be changed/updated
 *
 * @param <T>
 */
class MutableSignal<T> implements Signal<T> {
    private final Consumer<SignalLike<T>> readCallback;
    private final Set<Consumer<T>> observers = new HashSet<>();
    private final Consumer<Supplier<Set<Runnable>>> writeCallback;
    private final StackTraceElement trace;
    private T value;

    /**
     * @param readCallback  a function called when the signal is reactively read, param1: the thing that current effects need to subsribe to
     * @param writeCallback a function called when the signal is written to, the first parameter is the list of actions to be invoked as a result
     * @param value         the initial value of the signal
     */
    MutableSignal(Consumer<SignalLike<T>> readCallback, Consumer<Supplier<Set<Runnable>>> writeCallback, T value) {
        this.readCallback = readCallback;
        this.writeCallback = writeCallback;
        this.value = value;
        trace = Thread.currentThread().getStackTrace()[3];
    }

    @Override
    public void set(T i) {
        if (Objects.equals(i, value)) return;
        value = i;
        writeCallback.accept(()-> observers.stream().map(it -> (Runnable) () -> it.accept(i)).collect(Collectors.toSet()));
    }

    @Override
    public T get() {
        readCallback.accept(this);
        return value;
    }

    @Override
    public T getUntracked() {
        return value;
    }

    @Override
    public StackTraceElement getOrigin() {
        return trace;
    }

    @Override
    public Subscription onChange(Consumer<T> consumer) {
        this.observers.add(consumer);
        return () -> observers.remove(consumer);
    }

    @Override
    public Subscription onDirty(Consumer<SignalLike<T>> consumer) {
        return () -> {
        };
    }
}
