package eu.nitonfx.signaling.api;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

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

    /**
     * A untracked <b>read</b> iterator, writes such as {@link Iterator#remove()} will still be forwarded to the set and reacted to accordingly
     * <p>The purpose of this method is if you do not want the surounding context to re-execute when the set changes</p>
     */
    Iterator<E> untrackedIterator();

    /**
     * A read-only iterator. This method is reactively tracked so invoking this method will cause the surounding context
     * to be re-executed when the set is modified.
     * <p>The reason this is read only is that if it wasn't it would often cause recursion since the removal effect would modify the set which would cause the modify to run again. For a {@link Iterator} that supports writes/manipulation use {@link  #untrackedIterator()}</p>
     */
    @Override
    @NotNull Iterator<E> iterator();

    EffectHandle onAdd(Consumer<E> o);

    <N> SetSignal<N> map(Function<E, N> mapper);
}
