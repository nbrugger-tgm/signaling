package eu.nitonfx.signaling.api;

import java.util.List;

/**
 * A ListSignal is a reactive list that allows for tracking changes to its elements.
 * It extends the List interface and provides additional methods to get signals for individual elements
 * and to get the list of elements without tracking.
 *
 * @param <T> the type of elements in the list
 */
public interface ListSignal<T> extends List<T> {

    /**
     * Returns the signal for the element at the specified position in the list.
     *
     * @param index the index of the element
     * @return the signal for the element at the specified position
     */
    Signal<T> getSignal(int index);

    /**
     * @return the list of elements without tracking
     */
    List<T> getUntracked();
}
