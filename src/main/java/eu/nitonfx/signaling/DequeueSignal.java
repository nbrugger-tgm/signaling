package eu.nitonfx.signaling;

import eu.nitonfx.signaling.api.Signal;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedList;
import java.util.function.Consumer;

public class DequeueSignal<T> implements Subscribable, Signal<T> {
    private final Consumer<Subscribable> readCallback;
    private final Deque<Runnable> observers = new LinkedList<>();
    private T value;

    DequeueSignal(Consumer<Subscribable> readCallback, T value) {
        this.readCallback = readCallback;
        this.value = value;
    }

    public Subscription subscribe(Runnable observer) {
        observers.addFirst(observer);
        return () -> observers.removeIf(o -> o == observer);
    }

    @Override
    public void set(T i) {
        value = i;
        for (Runnable runnable : new ArrayDeque<>(observers)) {
            runnable.run();
        }
    }

    @Override
    public T get() {
        readCallback.accept(this);
        return value;
    }
}
