package eu.nitonfx.signaling.collections;

import eu.nitonfx.signaling.api.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class SetMapSignal<K, V> extends AbstractMap<K, V> implements MapSignal<K, V> {
    private final SetSignal<SignalEntry<K, V>> map;
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
        return map.stream().filter(e -> e.getKey().equals(key)).findFirst().map(e -> e.value).orElse(null);
    }

    @Override
    public Supplier<V> get(Supplier<? extends K> key) {
        var memo = cx.createMemo(()->getSignal(key.get()));
        return ()-> {
            var entry = memo.get();
            if(entry != null) return entry.get();
            else return null;
        };
    }

    @Override
    public @Nullable V put(K key, V value) {
        var existing = getSignal(key);
        if(existing != null) {
            var old = existing.getUntracked();
            existing.set(value);
            return old;
        } else {
            map.add(new SignalEntry<>(key, cx.createSignal(value)));
            return null;
        }
    }

    @Override
    public void putAll(@NotNull Map<? extends K, ? extends V> m) {
        m.forEach(this::put);
    }

    @Override
    public @NotNull Set<Entry<K, V>> entrySet() {
        return Collections.unmodifiableSet(map);
    }

    @Override
    @Unmodifiable
    public Map<K, V> getUntracked() {
        return Map.ofEntries(map.getUntracked().stream()
                .map(it -> Map.entry(it.getKey(), ((SignalEntry<K, V>) it.getValue()).getUntracked()))
                .toArray(Entry[]::new)
        );
    }

    @Override
    public @NotNull SetSignal<K> keySetSignal() {
        var signal = cx.createSignal(Set.<K>of());
        map.onAdd(it -> {
            signal.add(it.getKey());
            cx.cleanup(()->signal.remove(it.getKey()));
        });
        return signal;
    }

    @Override
    public EffectHandle onPut(BiConsumer<K, SignalLike<V>> o) {
        Map<K, EffectHandle> handles = new HashMap<>();
        map.getUntracked().forEach(it -> handles.put(it.getKey(), cx.createEffect(()->o.accept(it.getKey(), it.value))));
        map.onAdd((e) -> {
            var oldHandle = handles.put(e.getKey(), cx.createEffect(() -> o.accept(e.getKey(), e.value)));
            if(oldHandle != null) oldHandle.cancel();
        });
        return EffectHandle.of("MapOnPutEffect",()->{
            handles.forEach((k,e)->e.cancel());
            handles.clear();
        }, ()->handles.values().stream().map(it -> "|-"+it).collect(Collectors.joining("\n")));
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
