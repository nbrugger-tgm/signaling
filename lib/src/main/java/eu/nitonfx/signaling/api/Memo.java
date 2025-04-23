package eu.nitonfx.signaling.api;

import java.util.function.Supplier;

public interface Memo<T> extends Supplier<T> {
    void recompute();
}
