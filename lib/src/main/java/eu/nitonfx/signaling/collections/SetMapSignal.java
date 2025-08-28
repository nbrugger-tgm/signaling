package eu.nitonfx.signaling.collections;

import eu.nitonfx.signaling.api.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
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
        map = cx.createSignal(initial.entrySet().stream().map(e -> (SignalEntry<K,V>)new DefaultSignalEntry(cx, e)).collect(Collectors.toSet()));
    }

    @Override
    public SignalLike<V> getSignal(K key) {
        return map.stream()
                .filter(e -> e.getKey().equals(key))
                .findFirst()
                .map(SignalEntry::getValueSignal)
                .orElse(null);
    }

    @Override
    public SignalLike<V> get(Supplier<? extends K> key) {
        var memo = cx.createMemo(() -> getSignal(key.get()));
        return memo.map((entry) -> {
            if (entry != null) return entry.get();
            else return null;
        });
    }

    @Override
    public <N> MapSignal<K, N> mapValues(Function<V, N> mapper) {
        return new View<>(mapper);
    }

    private class View<N> extends AbstractMap<K, N> implements MapSignal<K, N> {
        private final Function<V, N> mapper;

        private View(Function<V, N> mapper) {
            this.mapper = mapper;
        }

        @Override
        public SignalLike<N> getSignal(K key) {
            return SetMapSignal.this.getSignal(key).map(mapper);
        }

        @Override
        public SignalLike<N> get(Supplier<? extends K> key) {
            return SetMapSignal.this.get(key).map(mapper);
        }

        @Override
        public <N1> MapSignal<K, N1> mapValues(Function<N, N1> mapper) {
            return new View<>(this.mapper.andThen(mapper));
        }

        @Override
        public Map<K, N> getUntracked() {
            return SetMapSignal.this.map.getUntracked().stream()
                    .collect(Collectors.toMap(
                            SignalEntry::getKey,
                            e -> mapper.apply(e.getValue())
                    ));
        }

        @Override
        public @NotNull SetSignal<K> keySetSignal() {
            return SetMapSignal.this.keySetSignal();
        }

        @Override
        public EffectHandle onPut(BiConsumer<K, SignalLike<N>> o) {
            return SetMapSignal.this.onPut((k, v) -> o.accept(k, v.map(mapper)));
        }

        @Override
        public @NotNull Set<Entry<K, N>> entrySet() {
            return map.map(it -> it.map(mapper));
        }
    }

    @Override
    public @Nullable V put(K key, V value) {
        var existing = getSignal(key);
        if (existing != null) {
            var old = existing.getUntracked();
            if(existing instanceof Signal<V> signal) // we know this because we created it ourselves
                signal.set(value);
            else throw new IllegalStateException("Expected a Signal but got a " + existing.getClass());
            return old;
        } else {
            map.add(new DefaultSignalEntry<>(key, cx.createSignal(value)));
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
        var untrackedEntrySet = map.getUntracked();
        return untrackedEntrySet.stream()
                .collect(Collectors.toMap(
                        Entry::getKey,
                        it -> it.getValueSignal().getUntracked()
                ));
    }

    @Override
    public @NotNull SetSignal<K> keySetSignal() {
        return map.map(Entry::getKey);
    }

    @Override
    public EffectHandle onPut(BiConsumer<K, SignalLike<V>> o) {
        Map<K, EffectHandle> handles = new HashMap<>();
        map.getUntracked().forEach(it -> handles.put(it.getKey(), cx.createEffect(() -> o.accept(it.getKey(), it.getValueSignal()))));
        map.onAdd((e) -> {
            var oldHandle = handles.put(e.getKey(), cx.createEffect(() -> o.accept(e.getKey(), e.getValueSignal())));
            if (oldHandle != null) oldHandle.cancel();
        });
        var effect = EffectHandle.of("MapOnPutEffect", () -> {
            handles.forEach((k, e) -> e.cancel());
            handles.clear();
        }, () -> handles.values().stream().map(it -> "|-" + it).collect(Collectors.joining("\n")));
        cx.registerEffect(effect);
        return effect;
    }

    public StackTraceElement getOrigin() {
        return origin;
    }

    private interface SignalEntry<K, V> extends Entry<K, V> {
        SignalLike<V> getValueSignal();

        Entry<K, V> getUntracked();

        <N> Entry<K, N> map(Function<V, N> mapper);
    }

    private record DefaultSignalEntry<K, V>(K key, Signal<V> value) implements SignalEntry<K, V> {

        public DefaultSignalEntry(Context cx, Entry<K, V> e) {
                this(e.getKey(), cx.createSignal(e.getValue()));
            }

            @Override
            public SignalLike<V> getValueSignal() {
                return value;
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

            public <N> Entry<K, N> map(Function<V, N> mapper) {
                return new View<>(mapper);
            }

            private class View<N> implements SignalEntry<K, N> {
                private final Function<V, N> mapper;

                private View(Function<V, N> mapper) {
                    this.mapper = mapper;
                }

                @Override
                public K getKey() {
                    return DefaultSignalEntry.this.getKey();
                }

                @Override
                public N getValue() {
                    return mapper.apply(DefaultSignalEntry.this.getValue());
                }

                @Override
                public N setValue(N value) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public SignalLike<N> getValueSignal() {
                    return DefaultSignalEntry.this.value.map(mapper);
                }

                @Override
                public Entry<K, N> getUntracked() {
                    return Map.entry(getKey(), mapper.apply(DefaultSignalEntry.this.value.getUntracked()));
                }

                @Override
                public <N1> Entry<K, N1> map(Function<N, N1> mapper) {
                    return new View<>(this.mapper.andThen(mapper));
                }
            }
        }
}
