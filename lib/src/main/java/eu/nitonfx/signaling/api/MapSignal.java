package eu.nitonfx.signaling.api;

import java.util.Map;

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
     * @return the map of entries without tracking
     */
    Map<K,V> getUntracked();
}
