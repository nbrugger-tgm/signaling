package eu.nitonfx.signaling;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class Effect implements Runnable {
    private final Runnable effect;
    private final Function<Runnable, EffectCapture> capturingExecutor;
    private Set<? extends Subscription> subscriptions = new HashSet<>();
    private List<Effect> nestedEffects = List.of();
    private Predicate<Subscribable> dependencyFilter = (e) -> true;
    private Set<Runnable> cleanup = new HashSet<>();

    public Effect(Runnable effect, Function<Runnable, EffectCapture> capturingExecutor) {
        this.effect = effect;
        this.capturingExecutor = capturingExecutor;
    }

    @Override
    public void run() {
        unsubscribe();
        var capture = capturingExecutor.apply(effect);
        cleanup = capture.cleanup();
        var dependencies = capture.dependencies().stream().filter(dependencyFilter).collect(Collectors.toSet());
        subscriptions = dependencies.stream().map(subscribable -> subscribable.subscribe(this)).collect(Collectors.toSet());
        var allNestedEffects = new ArrayList<>(capture.nestedEffects());
        allNestedEffects.forEach(nested -> nested.dependencyFilter = dependencyFilter.and(not(dep -> dependencies.stream().anyMatch(inner -> inner == dep))));

        //If the effect caused writes to signals, the effects attached to this signals are not run immediately, but deferred to the end of the current effect
        //This is the deffered execution of this effects
        Queue<Runnable> deferred = capture.flatDeferredEffects().collect(Collectors.toCollection(LinkedList::new));
        while (!deferred.isEmpty()) {
            var runnable = deferred.poll();
            if (!(runnable instanceof Effect effect)) {
                runnable.run();
                continue;
            }
            effect.unsubscribe();
            var innerCapture = capturingExecutor.apply(effect.effect);
            var innerDependencies = innerCapture.dependencies().stream().filter(effect.dependencyFilter).collect(Collectors.toSet());
            effect.subscriptions = innerDependencies.stream()
                    .filter(effect.dependencyFilter)
                    .map(subscribable -> subscribable.subscribe(effect))
                    .collect(Collectors.toSet());
            allNestedEffects.addAll(innerCapture.nestedEffects().stream().map(
                    nested -> {
                        nested.dependencyFilter = effect.dependencyFilter.and(not(innerDependencies::contains));
                        return nested;
                    }
            ).collect(Collectors.toSet()));
            deferred.addAll(innerCapture.flatDeferredEffects().toList());
        }
        allNestedEffects.forEach(Effect::run);
        nestedEffects = allNestedEffects;
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
}
