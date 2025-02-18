package eu.nitonfx.signaling;

import eu.nitonfx.signaling.api.SignalLike;

import java.util.Objects;

record Dependency<T>(
        SignalLike<T> signal,
        T lastValue
) {
    Dependency(SignalLike<T> sig) {
        this(sig, sig.getUntracked());
    }

    public boolean isChanged() {
        return !Objects.equals(signal.getUntracked(), lastValue);
    }
}
