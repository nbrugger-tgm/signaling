package eu.nitonfx.signaling.api;

import java.util.List;

public interface ListSignal<T> extends List<T>, Signal<List<T>> {
    Signal<T> getSignal(int index);
}
