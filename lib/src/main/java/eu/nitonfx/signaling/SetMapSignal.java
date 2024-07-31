package eu.nitonfx.signaling;

import eu.nitonfx.signaling.api.Context;
import eu.nitonfx.signaling.api.MapSignal;
import eu.nitonfx.signaling.api.Signal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class SetMapSignal<K,V>  extends AbstractMap<K, V> implements MapSignal<K,V> {
    private final Set<Entry<K,V>> map;
    private final Context cx;

    public SetMapSignal(Context cx) {
        this.cx = cx;
        map = cx.createSignal(new HashSet<>());
    }

    public SetMapSignal(Context cx, Map<K,V> initial) {
        this.cx = cx;
        map = cx.createSignal(initial.entrySet().stream().map(e -> new SignalEntry<>(cx,e)).collect(Collectors.toSet()));
    }

    @Override
    public Signal<V> getSignal(K key) {
        return map.stream().filter(e -> e.getKey().equals(key)).findFirst().map(e -> ((SignalEntry<K,V>)e).value).orElse(null);
    }

    @Override
    public @Nullable V put(K key, V value) {
        var removed = remove(key);
        map.add(new SignalEntry<>(key, cx.createSignal(value)));
        return removed;
    }

    @Override
    public void putAll(@NotNull Map<? extends K, ? extends V> m) {
        m.forEach(this::put);
    }

    @Override
    public @NotNull Set<Entry<K, V>> entrySet() {
        return map;
    }

    @Override
    public void set(Map<K, V> i) {
        clear();
        putAll(i);
    }

    @Override
    public Map<K, V> get() {
        return this;
    }

    @Override
    public Map<K, V> getUntracked() {
        return this;
    }

    public static class SignalEntry<K,V> implements Entry<K,V> {
    private final K key;
    private final Signal<V> value;

        private SignalEntry(K key, Signal<V> value) {
            this.key = key;
            this.value = value;
        }

        public SignalEntry(Context cx, Entry<K, V> e) {
            this(e.getKey(), cx.createSignal(e.getValue()));
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value.get();
        }

        @Override
        public V setValue(V value) {
            var old = this.value.get();
            this.value.set(value);
            return old;
        }
    }
}
