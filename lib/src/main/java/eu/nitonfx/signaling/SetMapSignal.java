package eu.nitonfx.signaling;

import eu.nitonfx.signaling.api.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class SetMapSignal<K, V> extends AbstractMap<K, V> implements MapSignal<K, V> {
    private final SetSignal<Entry<K, V>> map;
    private final Context cx;
    private final StackTraceElement origin;

    public SetMapSignal(Context cx, StackTraceElement origin) {
        this.cx = cx;
        map = cx.createSignal(new HashSet<>());
        this.origin = origin;
    }

    public SetMapSignal(Context cx, Map<K, V> initial, StackTraceElement origin) {
        this.cx = cx;
        this.origin = origin;
        map = cx.createSignal(initial.entrySet().stream().map(e -> new SignalEntry<>(cx, e)).collect(Collectors.toSet()));
    }

    @Override
    public Signal<V> getSignal(K key) {
        return map.stream().filter(e -> e.getKey().equals(key)).findFirst().map(e -> ((SignalEntry<K, V>) e).value).orElse(null);
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
    @Unmodifiable
    public Map<K, V> getUntracked() {
        return Map.ofEntries(map.getUntracked().stream()
                .map(it -> Map.entry(it.getKey(), ((SignalEntry<K, V>) it.getValue()).getUntracked()))
                .toArray(Entry[]::new)
        );
    }

    public StackTraceElement getOrigin() {
        return origin;
    }

    public static class SignalEntry<K, V> implements Entry<K, V> {
        private final K key;
        private final Signal<V> value;

        private SignalEntry(K key, Signal<V> value) {
            this.key = key;
            this.value = value;
        }

        public SignalEntry(Context cx, Entry<K, V> e) {
            this(e.getKey(), cx.createSignal(e.getValue()));
        }

        public Entry<K, V> getUntracked() {
            return Map.entry(key, value.getUntracked());
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
