package eu.nitonfx.signaling;

import eu.nitonfx.signaling.api.Context;
import eu.nitonfx.signaling.api.Signal;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public class SetStackContext implements Context {
    private final Set<Subscribable> dependencies = new HashSet<>();
    private final Set<Effect> nestedEffects = new HashSet<>();
    private final Set<Supplier<Set<Runnable>>> deferredEffects = new HashSet<>();
    private boolean recording = false;

    @Override
    public <T> Signal<T> createSignal(T initial) {
        return new DequeueSignal<>((subscribable) -> {
            if (!recording)
                throw new IllegalStateException("Signal was read outside of an effect!");
            dependencies.add(subscribable);
        }, (observers) -> {
            if (recording)
                deferredEffects.add(observers);
            else observers.get().forEach(Runnable::run);
        }, initial);
    }

    @Override
    public void createEffect(Runnable effect) {
        var effectWrapper = new Effect(effect, this::runAndCapture);
        if (recording) nestedEffects.add(effectWrapper);
        else effectWrapper.run();
    }

    private EffectCapture runAndCapture(Runnable effect) {
        dependencies.clear();
        nestedEffects.clear();
        deferredEffects.clear();
        recording = true;
        effect.run();
        recording = false;
        return new EffectCapture(copyOf(dependencies), copyOf(nestedEffects), copyOf(deferredEffects));
    }

    public static<T> Set<T> copyOf(Set<T> dependencies) {
        //Set.copyOf() not supported by teaVm
        return Collections.unmodifiableSet(new HashSet<>(dependencies));
    }


}
