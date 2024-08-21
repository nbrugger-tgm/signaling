package eu.nitonfx.signaling;

import eu.nitonfx.signaling.api.Context;
import eu.nitonfx.signaling.api.ListSignal;
import eu.nitonfx.signaling.api.Signal;
import org.jetbrains.annotations.NotNull;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ArraySignalList<T> extends AbstractList<T> implements ListSignal<T> {
    private final List<Signal<T>> list;
    private final Context cx;
    private final Signal<Integer> size;

    public ArraySignalList(Context cx) {
        this.cx = cx;
        size = cx.createSignal(0);
        list = new ArrayList<>();
    }
    public ArraySignalList(Context cx, List<T> initial) {
        this.cx = cx;
        this.list = initial.stream().map(cx::createSignal).collect(Collectors.toCollection(ArrayList::new));
        size = cx.createSignal(initial.size());
    }

    @Override
    public T get(int index) {
        return list.get(index).get();
    }

    @Override
    public void add(int index, T element) {
        list.add(index, cx.createSignal(element));
        size.set(list.size());
    }

    @Override
    public boolean add(T t) {
        final var added = list.add(cx.createSignal(t));
        size.set(list.size());
        return added;
    }

    @Override
    public T remove(int index) {
        final var removed = list.remove(index);
        size.set(list.size());
        return removed.get();
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
    public void set(List<T> i) {
        clear();
        addAll(i);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends T> c) {
        var res = list.addAll(((Collection<T>)c).stream().map(cx::createSignal).toList());
        size.set(list.size());
        return res;
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        var res = list.removeIf(signal -> c.contains(signal.get()));
        size.set(list.size());
        return res;
    }

    @Override
    public boolean removeIf(Predicate<? super T> filter) {
        var res = list.removeIf(signal -> filter.test(signal.get()));
        size.set(list.size());
        return res;
    }

    @Override
    public boolean remove(Object o) {
        var res = list.removeIf(signal -> signal.get().equals(o));
        size.set(list.size());
        return res;
    }

    @Override
    public T set(int index, T element) {
        var signal = getSignal(index);
        var old = signal.getUntracked();
        getSignal(index).set(element);
        return old;
    }

    @Override
    public List<T> get() {
        return this;
    }

    @Override
    public List<T> getUntracked() {
        return this;
    }
}
