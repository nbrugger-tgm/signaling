package eu.nitonfx.signaling;

import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

public record EffectCapture(
        Set<? extends Subscribable> dependencies,
        Set<Effect> nestedEffects,
        Set<Supplier<Set<Runnable>>> deferredEffects) {
    Stream<Runnable> flatDeferredEffects(){
        return deferredEffects().stream().map(Supplier::get).flatMap(Set::stream);
    }
}
