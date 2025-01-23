package eu.nitonfx.signaling;

import eu.nitonfx.signaling.api.SignalLike;
import eu.nitonfx.signaling.api.Subscription;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DerivedSignal<T> implements SignalLike<T> {
    private final Supplier<T> function;
    private final Consumer<SignalLike<T>> onReadListener;
    private final Function<Runnable, EffectCapture> captureFunction;
    private final StackTraceElement origin;
    private final Set<Consumer<T>> listeners = new HashSet<>();
    private final Set<Consumer<SignalLike<T>>> onDirtyListeners = new HashSet<>();
    private final Consumer<Supplier<Set<Runnable>>> writeCallback;
    private final Consumer<Supplier<Set<Runnable>>> dirtyCallback;
    private Set<Subscription> subscriptions = Set.of();
    private T cache = null;
    /**
     * indicates that an underlying dependency is dirty (might have changed)
     */
    private boolean dirty = true;
    /**
     * indicates that an underlying dependency has changed and this value NEEDS recalculation
     */
    private boolean confirmedDirty = true;
    private Set<Dependency<?>> dependencies = Set.of();


    public DerivedSignal(
            StackTraceElement origin,
            Supplier<T> function,
            Consumer<SignalLike<T>> onReadListener,
            Function<Runnable, EffectCapture> captureFunction,
            Consumer<Supplier<Set<Runnable>>> writeCallback,
            Consumer<Supplier<Set<Runnable>>> dirtyCallback
    ) {
        this.function = function;
        this.onReadListener = onReadListener;
        this.captureFunction = captureFunction;
        this.origin = origin;
        this.writeCallback = writeCallback;
        this.dirtyCallback = dirtyCallback;
    }

    @Override
    public T get() {
        onReadListener.accept(this);
        return getUntracked();
    }

    private void markDirty(boolean known) {
        var wasDirty = dirty;
        this.dirty = true;
        this.confirmedDirty = this.confirmedDirty || known;
        if (!wasDirty) {
            dirtyCallback.accept(() -> onDirtyListeners.stream().map(
                    c -> (Runnable) () -> c.accept(this)
            ).collect(Collectors.toSet()));
        }
    }

    @Override
    public T getUntracked() {
        if (!dirty) return cache;
        var dependenciesChanged = confirmedDirty || !dependencies.stream().allMatch(dep ->
                dep.lastValue == dep.signal.getUntracked() || dep.lastValue.equals(dep.signal.getUntracked())
        );
        if (!dependenciesChanged) {
            dirty = false;
            return cache;
        }
        return recalculate();
    }

    private T recalculate() {
        subscriptions.forEach(Subscription::unsubscribe);
        var oldValue = cache;
        var capture = captureFunction.apply(() -> cache = function.get());
        dependencies = capture.dependencies().stream().map(Dependency::new).collect(Collectors.toSet());
        subscriptions = dependencies.stream().flatMap(dep -> Stream.of(
                dep.signal.onDirty((__) -> markDirty(false)),
                dep.signal.onChange((__) -> markDirty(true))
        )).collect(Collectors.toSet());
        if (!capture.cleanup().isEmpty())
            throw new UnsupportedOperationException("cleanup not allowed in derived signal (%s): %s".formatted(this, capture.cleanup()));
        if (!capture.deferredEffects().isEmpty())
            throw new UnsupportedOperationException("side effects in derived signal (%s): %s".formatted(this, capture.deferredEffects()));
        if (!capture.nestedEffects().isEmpty())
            throw new UnsupportedOperationException("side effects in derived signal (%s): %s".formatted(this, capture.nestedEffects()));
        dirty = false;
        confirmedDirty = false;
        if (!Objects.equals(oldValue, cache)) {
            writeCallback.accept(() -> listeners.stream().map(
                    listener -> (Runnable) () -> listener.accept(cache)
            ).collect(Collectors.toSet()));
        }
        return cache;
    }

    @Override
    public StackTraceElement getOrigin() {
        return origin;
    }

    @Override
    public Subscription onChange(Consumer<T> listener) {
        this.listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    @Override
    public Subscription onDirty(Consumer<SignalLike<T>> listener) {
        this.onDirtyListeners.add(listener);
        return () -> listeners.remove(listener);
    }

    @Override
    public String toString() {
        return "DerivedSignal { value = %s, clean = %s } %s".formatted(
                cache, !dirty, origin
        );
    }

    private record Dependency<T>(
            SignalLike<T> signal,
            T lastValue
    ) {
        Dependency(SignalLike<T> signal) {
            this(signal, signal.getUntracked());
        }
    }
}
