package eu.nitonfx.signaling.api;

import java.util.Set;

/**
 * A SetSignal is a reactive set that allows for tracking changes to its elements.
 * It provides additional methods to get the set of elements without tracking.
 *
 * @param <E> the type of elements in the set
 */
public interface SetSignal<E> extends Set<E> {

    /**
     * Returns the set of elements without tracking.
     *
     * @return the set of elements without tracking
     */
    Set<E> getUntracked();
}
