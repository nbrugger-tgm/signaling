package eu.nitonfx.signaling.api;

import java.util.Map;

public interface MapSignal<K, V> extends Map<K,V>, Signal<Map<K,V>> {
    Signal<V> getSignal(K key);
}
