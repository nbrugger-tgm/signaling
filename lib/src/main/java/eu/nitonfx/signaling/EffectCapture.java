package eu.nitonfx.signaling;

import eu.nitonfx.signaling.api.EffectHandle;
import eu.nitonfx.signaling.api.SignalLike;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

public record EffectCapture(
        Set<Dependency<?>> dependencies,
        List<EffectHandle> nestedEffects,
        List<Supplier<Set<Runnable>>> deferredEffects,
        Set<Runnable> cleanup) {
    Stream<Runnable> flatDeferredEffects(){
        return deferredEffects().stream().map(Supplier::get).flatMap(Set::stream);
    }
}
