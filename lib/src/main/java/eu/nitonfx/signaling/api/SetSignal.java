package eu.nitonfx.signaling.api;

import java.util.Set;

public interface SetSignal<E> extends Set<E> {
    Set<E> getUntracked();
}
