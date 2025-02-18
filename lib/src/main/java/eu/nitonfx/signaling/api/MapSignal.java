package eu.nitonfx.signaling.api;

import java.util.Map;

public interface MapSignal<K, V> extends Map<K,V> {
    SignalLike<V> getSignal(K key);
    Map<K,V> getUntracked();
}
