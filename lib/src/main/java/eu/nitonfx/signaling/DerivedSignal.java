package eu.nitonfx.signaling;

import eu.nitonfx.signaling.api.Signal;
import eu.nitonfx.signaling.api.SignalLike;
import eu.nitonfx.signaling.api.Subscription;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class DerivedSignal<T> implements SignalLike<T> {
    private final Supplier<T> function;
    private final Consumer<SignalLike<T>> onReadListener;
    private final Function<Runnable, EffectCapture> captureFunction;
    private final StackTraceElement origin;
    private final Set<Consumer<SignalLike<T>>> onDirtyEffects = new HashSet<>();
    private final Consumer<Supplier<Set<Runnable>>> changeCallback;
    private final Set<Consumer<SignalLike<T>>> onDirtyPropagators = new HashSet<>();
    private final Set<Dependency<?>> dirtyDependencies = new HashSet<>();

    private Set<Subscription> subscriptions = Set.of();
    private boolean initialized = false;
    private T cache;

    public DerivedSignal(
            StackTraceElement origin,
            Supplier<T> function,
            Consumer<SignalLike<T>> onReadListener,
            Function<Runnable, EffectCapture> captureFunction,
            Consumer<Supplier<Set<Runnable>>> changeCallback
    ) {
        this.function = function;
        this.onReadListener = onReadListener;
        this.captureFunction = captureFunction;
        this.origin = origin;
        this.changeCallback = changeCallback;
    }

    @Override
    public T get() {
        onReadListener.accept(this);
        return getUntracked();
    }

    private void markDirty(Dependency<?> known) {
        var wasDirty = !dirtyDependencies.isEmpty();
        dirtyDependencies.add(known);
        if (!wasDirty) {
            onDirtyPropagators.forEach(c -> c.accept(this));
        }
    }

    @Override
    public T getUntracked() {
        if (!initialized) recalculate();
        else if (isDirty() && dirtyDependencies.stream().anyMatch(Dependency::isChanged)) recalculate();
        else dirtyDependencies.clear();

        return cache;
    }

    private void recalculate() {
        subscriptions.forEach(Subscription::unsubscribe);
        dirtyDependencies.clear();
        var capture = captureFunction.apply(() -> cache = function.get());
        initialized = true;
        subscriptions = capture.dependencies().stream().map(Dependency::new)
                .<Subscription>mapMulti((dep, next) -> {
                    next.accept(dep.signal.propagateDirty((__) -> markDirty(dep)));
                    next.accept(dep.signal.onDirtyEffect((__) -> queueDependeants(dep)));
                })
                .collect(Collectors.toSet());
        if (!capture.cleanup().isEmpty())
            throw new UnsupportedOperationException("cleanup not allowed in derived signal (%s): %s".formatted(this, capture.cleanup()));
        if (!capture.deferredEffects().isEmpty())
            throw new UnsupportedOperationException("side effects in derived signal (%s): %s".formatted(this, capture.deferredEffects()));
        if (!capture.nestedEffects().isEmpty())
            throw new UnsupportedOperationException("side effects in derived signal (%s): %s".formatted(this, capture.nestedEffects()));
    }

    private void queueDependeants(Dependency<?> dep) {
        changeCallback.accept(() -> onDirtyEffects.stream().map(
                c -> (Runnable) () -> c.accept(this)
        ).collect(Collectors.toSet()));
    }

    private boolean isDirty() {
        return !dirtyDependencies.isEmpty();
    }


    @Override
    public StackTraceElement getOrigin() {
        return origin;
    }

    @Override
    public Subscription onDirtyEffect(Consumer<SignalLike<T>> effect) {
        this.onDirtyEffects.add(effect);
        return () -> onDirtyEffects.remove(effect);
    }

    @Override
    public Subscription propagateDirty(Consumer<SignalLike<T>> propagate) {
        this.onDirtyPropagators.add(propagate);
        return () -> onDirtyPropagators.remove(propagate);
    }

    @Override
    public String toString() {
        return "DerivedSignal { value = %s, clean = %s } %s".formatted(
                cache, !isDirty(), origin
        );
    }

    private record Dependency<T>(
            SignalLike<T> signal,
            T lastValue
    ) {
        Dependency(SignalLike<T> signal) {
            this(signal, signal.getUntracked());
        }

        public boolean isChanged() {
            return !Objects.equals(lastValue, signal.getUntracked());
        }
    }
}
