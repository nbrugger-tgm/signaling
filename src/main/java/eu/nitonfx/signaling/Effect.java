package eu.nitonfx.signaling;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;

public class Effect implements Runnable, Subscription {
    private final Runnable effect;
    private final Function<Runnable, EffectCapture> capturingExecutor;
    private Set<? extends Subscription> subscriptions = Set.of();
    private Set<? extends Subscription> nestedEffects = Set.of();
    private Predicate<Subscribable> dependencyFilter = (e) -> true;

    public Effect(Runnable effect, Function<Runnable, EffectCapture> capturingExecutor) {
        this.effect = effect;
        this.capturingExecutor = capturingExecutor;
    }

    @Override
    public void run() {
        var capture = capturingExecutor.apply(effect);
        unsubscribe();
        var dependencies = capture.dependencies().stream().filter(dependencyFilter).collect(Collectors.toSet());
        subscriptions = dependencies.stream().map(subscribable -> subscribable.subscribe(this)).collect(Collectors.toSet());
        var allNestedEffects = new HashSet<>(capture.nestedEffects());
        allNestedEffects.forEach(nested -> nested.dependencyFilter = dependencyFilter.and(not(dependencies::contains)));
        Queue<Runnable> deferred = capture.flatDeferredEffects().collect(Collectors.toCollection(LinkedList::new));
        while (!deferred.isEmpty()) {
            var runnable = deferred.poll();
            if (!(runnable instanceof Effect effect)) {
                runnable.run();
                continue;
            }
            var innerCapture = capturingExecutor.apply(effect.effect);
            effect.unsubscribe();
            var innerDependencies = innerCapture.dependencies().stream().filter(effect.dependencyFilter).collect(Collectors.toSet());
            effect.subscriptions = innerDependencies.stream().filter(effect.dependencyFilter).map(subscribable -> subscribable.subscribe(effect)).collect(Collectors.toSet());
            allNestedEffects.addAll(innerCapture.nestedEffects().stream().map(
                    nested -> {
                        nested.dependencyFilter = effect.dependencyFilter.and(not(innerDependencies::contains));
                        return nested;
                    }
            ).collect(Collectors.toSet()));
            deferred.addAll(innerCapture.flatDeferredEffects().toList());
        }
        allNestedEffects.forEach(Effect::run);
    }


    public void unsubscribe() {
        for (Subscription subscription : subscriptions) {
            subscription.unsubscribe();
        }
        subscriptions = Collections.emptySet();
        for (Subscription nestedEffect : nestedEffects) {
            nestedEffect.unsubscribe();
        }
        nestedEffects = Collections.emptySet();
    }
}
