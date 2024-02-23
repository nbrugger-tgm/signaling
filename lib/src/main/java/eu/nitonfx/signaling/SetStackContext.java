package eu.nitonfx.signaling;

import eu.nitonfx.signaling.api.Context;
import eu.nitonfx.signaling.api.ListSignal;
import eu.nitonfx.signaling.api.Signal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class SetStackContext implements Context {
    private final Set<Subscribable> dependencies = new HashSet<>();
    private final List<Effect> nestedEffects = new ArrayList<>(3);
    private final List<Supplier<Set<Runnable>>> deferredSignalUpdate = new ArrayList<>(8);
    private final Set<Runnable> cleanup = new HashSet<>();
    private boolean recording = false;

    @Override
    public <T> Signal<T> createSignal(T initial) {
        return new DequeueSignal<>((subscribable) -> {
            if (!recording)
                return;
//                throw new IllegalStateException("Signal was read outside of an effect!");
            dependencies.add(subscribable);
        }, (observers) -> {
            if (recording)
                deferredSignalUpdate.add(observers);
            else for (Runnable runnable : observers.get()) {
                runnable.run();
            }
        }, initial);
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
    public void createEffect(Runnable effect) {
        var effectWrapper = new Effect(effect, this::runAndCapture);
        if (recording) nestedEffects.add(effectWrapper);
        else effectWrapper.run();
    }

    @Override
    public void cleanup(Runnable func) {
        if (!recording)
            throw new IllegalStateException("Cleanup was called outside of an effect!");
        cleanup.add(func);
    }

    private EffectCapture runAndCapture(Runnable effect) {
        dependencies.clear();
        nestedEffects.clear();
        deferredSignalUpdate.clear();
        cleanup.clear();
        recording = true;
        effect.run();
        recording = false;
        return new EffectCapture(Set.copyOf(dependencies), List.copyOf(nestedEffects), List.copyOf(deferredSignalUpdate), Set.copyOf(cleanup));
    }
}
