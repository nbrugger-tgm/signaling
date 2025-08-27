package eu.nitonfx.signaling.collections;

import eu.nitonfx.signaling.api.*;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ArraySignalList<T> extends AbstractList<T> implements ListSignal<T> {
    private final List<Signal<T>> list;
    private final Context cx;
    private final Signal<Integer> size;
    private final StackTraceElement origin;
    private final List<Reconciler<Signal<T>>> reconcilers = new ArrayList<>();
    private String name;

    private interface Reconciler<T> {
        void onAdd(T element, int index);
        void onRemove(int index);
    }

    public ArraySignalList(Context cx, StackTraceElement origin) {
        this.cx = cx;
        size = cx.createSignal(0);
        size.setName("ListSignal.size");
        this.origin = origin;
        list = new ListenableList<>(new ArrayList<>(), this::onRemove, this::onAdd, null);
    }

    public ArraySignalList(Context cx, List<T> initial, StackTraceElement origin) {
        this.cx = cx;
        this.origin = origin;
        this.list = new ListenableList<>(initial.stream().map(cx::createSignal).collect(Collectors.toCollection(ArrayList::new)), this::onRemove, this::onAdd, null);
        size = cx.createSignal(initial.size());
    }

    private void onAdd(Signal<T> tSignal, Integer integer) {
        size.set(list.size());
        reconcilers.forEach(reconciler -> reconciler.onAdd(tSignal, integer));
    }

    private void onRemove(Integer integer) {
        size.set(list.size());
        reconcilers.forEach(reconciler -> reconciler.onRemove(integer));
    }

    @Override
    public T get(int index) {
        return list.get(index).get();
    }

    @Override
    public void add(int index, T element) {
        list.add(index, cx.createSignal(element));
    }

    @Override
    public T remove(int index) {
        return list.remove(index).get();
    }

    @Override
    public int size() {
        return size.get();
    }

    @Override
    public Signal<T> getSignal(int index) {
        return list.get(index);
    }

    @Override
    public T set(int index, T element) {
        var signal = getSignal(index);
        var old = signal.getUntracked();
        getSignal(index).set(element);
        return old;
    }

    @Override
    public List<T> getUntracked() {
        return list.stream().map(Signal::getUntracked).collect(Collectors.toList());
    }

    public EffectHandle onAdd(BiConsumer<SignalLike<T>, Integer> consumer) {
        List<EffectHandle> handles = new ArrayList<>();
        for (var i = 0; i < list.size(); i++) {
            int index = i;
            final var signal = getSignal(index);
            handles.add(cx.untracked(()-> {
                var effect = cx.createEffect(() -> consumer.accept(signal, index));
                effect.name(name + ".onAdd[" + index + "]");
                return effect;
            }));
        }
        var reconciler = new Reconciler<Signal<T>>() {
            @Override
            public void onAdd(Signal<T> element, int index) {
                var effect = cx.createEffect(() -> consumer.accept(element, index));
                effect.name(name + ".onAdd[" + index + "]");
                handles.add(index, effect);
            }

            @Override
            public void onRemove(int index) {
                handles.get(index).cancel();
                handles.remove(index);
            }
        };
        reconcilers.add(reconciler);
        var name = this.name+".onAdd";
        var handle = EffectHandle.of(name, () -> {
            reconcilers.remove(reconciler);
            handles.forEach(EffectHandle::cancel);
            handles.clear();
        }, () -> handles.stream()
                .flatMap(it-> Stream.of(("-"+it.formatAsTree()).split("\n")))
                .map(it -> "|"+it)
                .collect(Collectors.joining("\n")));
        cx.registerEffect(handle);
        return handle;
    }

    @Override
    public void setName(String name) {
        this.name = name;
        size.setName(name+".size");
    }

    public StackTraceElement getOrigin() {
        return origin;
    }

    @Override
    public <N> ListSignal<N> map(Function<T, N> mapper) {
        return new MappedView<>(mapper, this);
    }

    private static class MappedView<O,N> extends AbstractList<N> implements ListSignal<N>{
        private final Function<O,N> mapper;
        private final ListSignal<O> unmapped;
        private String name;
        private MappedView(Function<O, N> mapper, ListSignal<O> unmapped) {
            this.mapper = mapper;
            this.unmapped = unmapped;
        }

        private class MappedSignal implements SignalLike<N>{
            private final SignalLike<O> source;

            private MappedSignal(SignalLike<O> source) {
                this.source = source;
            }

            @Override
            public N get() {
                return mapper.apply(source.get());
            }

            @Override
            public N getUntracked() {
                return mapper.apply(source.getUntracked());
            }

            @Override
            public Subscription onDirtyEffect(Consumer<SignalLike<N>> consumer) {
                return source.onDirtyEffect(signal -> consumer.accept(this));
            }

            @Override
            public Subscription propagateDirty(Consumer<SignalLike<N>> consumer) {
                return source.propagateDirty(signal -> consumer.accept(this));
            }
        }

        @Override
        public SignalLike<N> getSignal(int index) {
            return new MappedSignal(unmapped.getSignal(index));
        }

        @Override
        public List<N> getUntracked() {
            return unmapped.getUntracked().stream().map(mapper).collect(Collectors.toList());
        }

        @Override
        public EffectHandle onAdd(BiConsumer<SignalLike<N>, Integer> consumer) {
            return unmapped.onAdd((elem, index) -> consumer.accept(new MappedSignal(elem), index));
        }

        @Override
        public <M> ListSignal<M> map(Function<N, M> mapper) {
            return new MappedView<>(mapper, this);
        }

        @Override
        public void setName(String name) {
            this.name = name;
        }

        @Override
        public N get(int index) {
            return mapper.apply(unmapped.get(index));
        }

        @Override
        public int size() {
            return unmapped.size();
        }
    }
}
