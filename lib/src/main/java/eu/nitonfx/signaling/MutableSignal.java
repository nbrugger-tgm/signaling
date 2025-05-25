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
    private final List<Consumer<SignalLike<T>>> effects = new LinkedList<>();
    private final List<Consumer<SignalLike<T>>> instantObservers = new LinkedList<>();

    private final Consumer<Supplier<Set<Runnable>>> writeCallback;
    private final Consumer<SignalLike<T>> readCallback;

    private String name;
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
    }

    @Override
    public void set(T i) {
        if (Objects.equals(i, value)) return;
        value = i;
        instantObservers.forEach(observer -> observer.accept(this));
        writeCallback.accept(() -> effects.stream()
                .map(it -> (Runnable) () -> it.accept(this))
                .collect(Collectors.toSet())
        );
    }

    @Override
    public T get() {
        readCallback.accept(this);
        return value;
    }

    @Override
    public String toString() {
        return "MutableSignal(" + name + ")";
    }

    @Override
    public T getUntracked() {
        return value;
    }

    @Override
    public Subscription onDirtyEffect(Consumer<SignalLike<T>> consumer) {
        this.effects.add(consumer);
        return () -> effects.remove(consumer);
    }

    @Override
    public Subscription propagateDirty(Consumer<SignalLike<T>> consumer) {
        this.instantObservers.add(consumer);
        return () -> instantObservers.remove(consumer);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
