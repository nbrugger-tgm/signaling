package eu.nitonfx.signaling.api;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

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
     * This signal fires an update when the element at the given index is replaced.
     *
     * @param index the index of the element
     * @return the signal for the element at the specified position
     */
    SignalLike<T> getSignal(int index);

    /**
     * @return the list of elements without tracking
     */
    List<T> getUntracked();

    /**
     * <p>
     * Create an effect for each element added to the list at any time.
     * This is if you want to reactively want to mirror the elements of the list at another place.
     * This is the most atomic/reactive way to deal with a list.
     * </p>
     * <p>
     * It provides full reconciliation, when elements are removed from the list associated effects are cleaned canceled.
     * </p>
     * Order changes from insertions or deletions are not reflected.
     * Given the list {@code [A, B, C]} the effect will be called 3 times
     * ({@code consumer(A, 0)},{@code consumer(B, 1)} and {@code consumer(A, 2)})
     * when element B is then removed the effect of {@code consumer(B, 1)} is cleaned up but A and C stay untouched.
     * Therefore, the index should only be considered to be correct at the time of calling
     * @param consumer a listener that executes code for each element in the list now and in the future.
     *                the first parameter is the signal/value of the element while the second parameter is the element index
     * @return a handle to manually cancel all related effects and subscriptions
     */
    EffectHandle onAdd(BiConsumer<SignalLike<T>, Integer> consumer);


    /**
     * Set name for tracking/debugging purposes
     */
    void setName(String name);
}
