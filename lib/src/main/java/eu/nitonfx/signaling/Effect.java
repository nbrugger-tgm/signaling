package eu.nitonfx.signaling;

import eu.nitonfx.signaling.api.SignalLike;
import eu.nitonfx.signaling.api.Subscription;
import eu.nitonfx.signaling.api.EffectHandle;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Effect implements Runnable, EffectHandle {
    private final Runnable effect;
    private final Function<Runnable, EffectCapture> capturingExecutor;
    private final Consumer<Effect> effectExecutor;
    private final Consumer<EffectHandle> postExecuteHook;
    private Set<? extends Subscription> subscriptions = new HashSet<>();
    private Set<Dependency<?>> dependencies = Set.of();
    private List<? extends EffectHandle> nestedEffects = List.of();
    private Predicate<SignalLike<?>> dependencyFilter = (e) -> true;
    private Set<Runnable> cleanup = new HashSet<>();
    private final StackTraceElement trace;
    private String name = null;

    @Override
    public String formatAsTree() {
        return this+"\n"+
                "  |-Dependencies\n"+
                dependencies.stream().map(it -> "  |  |-"+it+"\n").collect(Collectors.joining())+
                "  |-Nested Effects\n"+
                nestedEffects.stream()
                        .flatMap(it->Stream.of(("-"+it.formatAsTree()).split("\n")))
                        .map(it -> "     |"+it)
                        .collect(Collectors.joining("\n"));
    }

    @Override
    public String toString() {
        return name != null ? "Effect(%s)".formatted(name) : "Effect from: "+trace.toString();
    }

    public Effect(Runnable effect, Function<Runnable, EffectCapture> capturingExecutor, Consumer<Effect> effectExecutor, Consumer<EffectHandle> postExecuteHook) {
        this.effect = effect;
        this.capturingExecutor = capturingExecutor;
        this.effectExecutor = effectExecutor;
        this.postExecuteHook = postExecuteHook;
        this.trace = Thread.currentThread().getStackTrace()[3];
    }

    @Override
    public void run() {
        unsubscribe();
        var capture = capturingExecutor.apply(effect);
        cleanup = capture.cleanup();
        this.dependencies = capture.dependencies().stream()
                .filter(dep -> dependencyFilter.test(dep.signal()))
                .collect(Collectors.toSet());
        var dependencySignals = dependencies.stream().map(Dependency::signal).toList();
        subscriptions = dependencies.stream()
                .map(dependency -> dependency.signal().onDirtyEffect((__)->runIfDependencyChanged(dependency)))
                .collect(Collectors.toSet());
        var nestedEffectHandles = capture.nestedEffects();
        var nestedEffects = nestedEffectHandles.stream()
                .filter(Effect.class::isInstance)
                .map(Effect.class::cast)
                .toList();
        final var filterForNestedEffects = dependencyFilter.and(Predicate.not(dep -> dependencySignals.stream().anyMatch(inner -> inner == dep)));
        nestedEffects.forEach(nested -> nested.dependencyFilter = filterForNestedEffects);

        //If the effect caused writes to signals, the effects attached to this signals are not run immediately, but deferred to the end of the current effect
        //This is the deferred execution of this effects
        Stream.concat(nestedEffects.stream(),capture.flatDeferredEffects()).forEach(Runnable::run);
        this.nestedEffects = nestedEffectHandles;
        postExecuteHook.accept(this);
    }

    private <T> void runIfDependencyChanged(Dependency<T> dependency) {
        if(dependency.isChanged()) effectExecutor.accept(this);
    }

    public void unsubscribe() {
        for (Subscription subscription : subscriptions) {
            subscription.unsubscribe();
        }
        subscriptions = Collections.emptySet();
        for (EffectHandle nestedEffect : nestedEffects) {
            nestedEffect.cancel();
        }
        nestedEffects = Collections.emptyList();

        cleanup.forEach(Runnable::run);
        cleanup = Collections.emptySet();
    }

    @Override
    public void cancel() {
        unsubscribe();
    }

    @Override
    public void name(String name) {
        this.name = name;
    }

}
