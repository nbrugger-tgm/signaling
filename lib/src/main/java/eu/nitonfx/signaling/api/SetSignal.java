package eu.nitonfx.signaling.api;

import java.util.Set;
import java.util.function.Consumer;

/**
 * A SetSignal is a reactive set that allows for tracking changes to its elements.
 * It provides additional methods to get the set of elements without tracking.
 *
 * @param <E> the type of elements in the set
 */
public interface SetSignal<E> extends Set<E> {

    /**
     * Returns the set of elements without tracking reads.
     *
     * Writes <i>might</i> still be forwarded to the signal list and trigger effects or not.
     * This means that it is not recommended to modify the returned value
     *
     * @return the set of elements without tracking reads
     */
    Set<E> getUntracked();

    EffectHandle onAdd(Consumer<E> o);
}
