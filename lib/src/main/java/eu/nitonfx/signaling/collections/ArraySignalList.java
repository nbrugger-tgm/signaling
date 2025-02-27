package eu.nitonfx.signaling.collections;

import eu.nitonfx.signaling.api.*;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class ArraySignalList<T> extends AbstractList<T> implements ListSignal<T> {
    private final List<Signal<T>> list;
    private final Context cx;
    private final Signal<Integer> size;
    private final StackTraceElement origin;
    private final List<Reconciler<Signal<T>>> reconcilers = new ArrayList<>();

    private interface Reconciler<T> {
        void onAdd(T element, int index);
        void onRemove(int index);
    }

    public ArraySignalList(Context cx, StackTraceElement origin) {
        this.cx = cx;
        size = cx.createSignal(0);
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
            handles.add(cx.createEffect(()->consumer.accept(signal, index)));
        }
        var reconciler = new Reconciler<Signal<T>>() {
            @Override
            public void onAdd(Signal<T> element, int index) {
                handles.add(index, cx.createEffect(() -> consumer.accept(element, index)));
            }

            @Override
            public void onRemove(int index) {
                handles.get(index).cancel();
                handles.remove(index);
            }
        };
        reconcilers.add(reconciler);
        return ()->{
            reconcilers.remove(reconciler);
            handles.forEach(EffectHandle::cancel);
            handles.clear();
        };
    }

    public StackTraceElement getOrigin() {
        return origin;
    }
}
