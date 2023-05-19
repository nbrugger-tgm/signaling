package eu.nitonfx.signaling;

import java.util.Collections;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Effect implements Runnable, Subscription {
    private final Runnable effect;
    private final Function<Runnable, EffectCapture> capturingExecutor;
    private Set<? extends Subscription> subscriptions = Set.of(()->{});//simulate single dependency because no-dependency Effects are not executed
    private Set<? extends Subscription> nestedEffects = Set.of();

    public Effect(Runnable effect, Function<Runnable, EffectCapture> capturingExecutor) {
        this.effect = effect;
        this.capturingExecutor = capturingExecutor;
    }

    @Override
    public void run() {
        if(subscriptions.isEmpty()) return;
        unsubscribe();
        var capture = capturingExecutor.apply(effect);
        nestedEffects = capture.nestedEffects();
        subscriptions = capture.dependencies().stream()
                .map(dependency -> dependency.subscribe(this))
                .collect(Collectors.toSet());
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
