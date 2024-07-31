package eu.nitonfx.signaling;

import eu.nitonfx.signaling.api.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;

public class SetStackContext implements Context {
    private final Set<Subscribable> dependencies = new HashSet<>();
    private final List<Effect> nestedEffects = new ArrayList<>(3);
    private final List<Supplier<Set<Runnable>>> deferredSignalUpdate = new ArrayList<>(8);
    private final Set<Runnable> cleanup = new HashSet<>();
    @Nullable
    private Runnable recording = null;

    /**
     * @param <T> The type of the signal, may not be a specific implementation of a container (ArrayList, HashSet, etc.)
     * @throws ClassCastException when {@code <T>} is a specific implementation of a container
     */
    @Override
    public <T> Signal<T> createSignal(T initial) {
        T container = switch (initial) {
            case null -> null;
            case List<?> list -> (T) createSignal(list);
            case Set<?> set -> (T) new HashSetSignal<>(this, set);
            default  -> initial;
        };
        var creationEffect = recording;
        return new DequeueSignal<>((subscribable) -> {
            if (recording == null)
                return;
//                throw new IllegalStateException("Signal was read outside of an effect!");
            if(recording == creationEffect)
                return;
            dependencies.add(subscribable);
        }, (observers) -> {
            if (recording != null)
                deferredSignalUpdate.add(observers);
            else for (Runnable runnable : observers.get()) {
                runnable.run();
            }
        }, container);
    }

    @Override
    public <T> ListSignal<T> createSignal(List<T> initial) {
        return new ArraySignalList<>(this, initial);
    }

    @Override
    public <T> ListSignal<T> createSignal(T[] initial) {
        return new ArraySignalList<>(this, List.of(initial));
    }

    @Override
    public <T> SetSignal<T> createSignal(Set<T> initial) {
        return new HashSetSignal<>(this, initial);
    }

    @Override
    public <K, V> MapSignal<K, V> createSignal(Map<K, V> initial) {
        return new SetMapSignal<>(this, initial);
    }

    @Override
    public <T> Supplier<T> createMemo(@NotNull Supplier<T> function) {
        return new MemoSignal<>(this, function, null);
    }
    @Override
    public <T> Supplier<T> createMemo(T init, @NotNull Supplier<T> function) {
        return new MemoSignal<>(this, function, init);
    }

    @Override
    public void createEffect(Runnable effect) {
        var effectWrapper = new Effect(effect, this::runAndCapture);
        if (recording != null) nestedEffects.add(effectWrapper);
        else effectWrapper.run();
    }

    @Override
    public void cleanup(Runnable func) {
        if (recording  == null)
            throw new IllegalStateException("Cleanup was called outside of an effect!");
        cleanup.add(func);
    }

    private EffectCapture runAndCapture(Runnable effect) {
        dependencies.clear();
        nestedEffects.clear();
        deferredSignalUpdate.clear();
        cleanup.clear();
        recording = effect;
        effect.run();
        recording = null;
        return new EffectCapture(Set.copyOf(dependencies), List.copyOf(nestedEffects), List.copyOf(deferredSignalUpdate), Set.copyOf(cleanup));
    }
}
