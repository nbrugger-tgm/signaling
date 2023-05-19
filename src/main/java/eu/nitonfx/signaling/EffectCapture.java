package eu.nitonfx.signaling;

import java.util.Set;

public record EffectCapture(
        Set<? extends Subscribable> dependencies,
        Set<? extends Subscription> nestedEffects
) {}
