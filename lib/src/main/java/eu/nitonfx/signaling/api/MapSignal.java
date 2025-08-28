package eu.nitonfx.signaling.api;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A MapSignal is a reactive map that allows for tracking changes to its entries.
 * It provides additional methods to get signals for individual entries
 * and to get the map of entries without tracking.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 */
public interface MapSignal<K, V> extends Map<K,V> {

    /**
     * Returns the signal for the value associated with the specified key in the map.
     *
     * @param key the key whose associated value's signal is to be returned
     * @return the signal for the value associated with the specified key, or null if the map contains no mapping for the key
     */
    SignalLike<V> getSignal(K key);

    /**
     * Similar to {@link #getSignal(Object)} but based on a dynamic/reactive key where the returned signal is updated
     * when the key supplier does
     * @param key a function to provide a key to look up. Should be a {@link SignalLike} or at least be based on one
     * @return a signal that changes when the key or the value the key points to changes
     */
    SignalLike<V> get(Supplier<? extends K> key);

    <N> MapSignal<K,N> mapValues(Function<V,N> mapper);

    /**
     * @return the map of entries without tracking
     */
    Map<K,V> getUntracked();

    @NotNull SetSignal<K> keySetSignal();

    EffectHandle onPut(BiConsumer<K, SignalLike<V>> o);
}
