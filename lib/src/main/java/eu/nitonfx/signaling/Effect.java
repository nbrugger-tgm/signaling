package eu.nitonfx.signaling;

import eu.nitonfx.signaling.api.SignalLike;
import eu.nitonfx.signaling.api.Subscription;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Effect implements Runnable {
    private final Runnable effect;
    private final Function<Runnable, EffectCapture> capturingExecutor;
    private Set<? extends Subscription> subscriptions = new HashSet<>();
    private Set<Dependency> dependencies = Set.of();
    private List<Effect> nestedEffects = List.of();
    private Predicate<SignalLike<?>> dependencyFilter = (e) -> true;
    private Set<Runnable> cleanup = new HashSet<>();
    private final StackTraceElement trace;

    @Override
    public String toString() {
        return trace.toString();
    }

    public Effect(Runnable effect, Function<Runnable, EffectCapture> capturingExecutor) {
        this.effect = effect;
        this.capturingExecutor = capturingExecutor;
        this.trace = Thread.currentThread().getStackTrace()[3];
    }

    @Override
    public void run() {
        unsubscribe();
        var capture = capturingExecutor.apply(effect);
        cleanup = capture.cleanup();
        var dependencies = capture.dependencies().stream().filter(dependencyFilter).collect(Collectors.toSet());
        subscriptions = Stream.concat(
                dependencies.stream().map(subscribable -> subscribable.onChange((__)->run())),
                dependencies.stream().map(subscribable -> subscribable.onDirty((__)->runIfDependenciesChanged()))
        ).collect(Collectors.toSet());
        var nestedEffects = capture.nestedEffects();
        final var filterForNestedEffects = dependencyFilter.and(not(dep -> dependencies.stream().anyMatch(inner -> inner == dep)));
        nestedEffects.forEach(nested -> nested.dependencyFilter = filterForNestedEffects);

        //If the effect caused writes to signals, the effects attached to this signals are not run immediately, but deferred to the end of the current effect
        //This is the deffered execution of this effects
        Stream.concat(nestedEffects.stream(),capture.flatDeferredEffects()).forEach(Runnable::run);
        this.nestedEffects = nestedEffects;
    }

    private void runIfDependenciesChanged(){
        if(dependencies.isEmpty() || areDependenciesChanged()) run();
    }

    private boolean areDependenciesChanged() {
        return !dependencies.stream().allMatch(it -> Objects.equals(it.lastValue, it.sig.getUntracked()));
    }

    //Predicate.not() is not available in TeaVM
    private<T> Predicate<T> not(Predicate<T> predicate) {
        return o -> !predicate.test(o);
    }


    public void unsubscribe() {
        for (Subscription subscription : subscriptions) {
            subscription.unsubscribe();
        }
        subscriptions = Collections.emptySet();
        for (Effect nestedEffect : nestedEffects) {
            nestedEffect.unsubscribe();
        }
        nestedEffects = Collections.emptyList();

        cleanup.forEach(Runnable::run);
        cleanup = Collections.emptySet();
    }

    private record Dependency(
            SignalLike<?> sig,
            Object lastValue
    ) { }
}
