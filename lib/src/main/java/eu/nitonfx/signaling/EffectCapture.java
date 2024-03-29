package eu.nitonfx.signaling;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

public record EffectCapture(
        Set<? extends Subscribable> dependencies,
        List<Effect> nestedEffects,
        List<Supplier<Set<Runnable>>> deferredEffects,
        Set<Runnable> cleanup) {
    Stream<Runnable> flatDeferredEffects(){
        return deferredEffects().stream().map(Supplier::get).flatMap(Set::stream);
    }
}
