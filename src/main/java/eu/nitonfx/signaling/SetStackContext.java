package eu.nitonfx.signaling;

import eu.nitonfx.signaling.api.Context;
import eu.nitonfx.signaling.api.Signal;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

public class SetStackContext implements Context {
    private final Stack<Set<Subscribable>> dependencyStack = new Stack<>();
    private final Stack<Set<Subscription>> effectStack = new Stack<>();

    @Override
    public <T> Signal<T> createSignal(T initial) {
        return new DequeueSignal<>((subscribable) -> {
            if (dependencyStack.isEmpty())
                throw new IllegalStateException("Signal was read outside of an effect!");
            var current = dependencyStack.peek();
            current.add(subscribable);
        }, initial);
    }

    @Override
    public void createEffect(Runnable effect) {
        var effectWrapper = new Effect(effect, this::runAndCapture);
        if (!effectStack.isEmpty()) effectStack.peek().add(effectWrapper);
        effectWrapper.run();
    }

    private EffectCapture runAndCapture(Runnable effect) {
        dependencyStack.push(new HashSet<>(1));
        effectStack.push(new HashSet<>(0));
        effect.run();
        var dependencies = dependencyStack.pop();
        var childEffects = effectStack.pop();
        return new EffectCapture(dependencies, childEffects);
    }
}
