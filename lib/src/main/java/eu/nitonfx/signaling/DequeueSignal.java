package eu.nitonfx.signaling;

import eu.nitonfx.signaling.api.Signal;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static eu.nitonfx.signaling.SetStackContext.copyOf;

public class DequeueSignal<T> implements Subscribable, Signal<T> {
    private final Consumer<Subscribable> readCallback;
    private final Set<Runnable> observers = new HashSet<>();
    private final Consumer<Supplier<Set<Runnable>>> writeCallback;
    private T value;

    DequeueSignal(Consumer<Subscribable> readCallback, Consumer<Supplier<Set<Runnable>>> writeCallback, T value) {
        this.readCallback = readCallback;
        this.writeCallback = writeCallback;
        this.value = value;
    }

    public Subscription subscribe(Runnable observer) {
        observers.add(observer);
        return () -> observers.remove(observer);
    }

    @Override
    public void set(T i) {
        if(i == value) return;
        value = i;
        writeCallback.accept(()->copyOf(observers));
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
}
