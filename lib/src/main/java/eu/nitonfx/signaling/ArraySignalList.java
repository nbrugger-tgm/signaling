package eu.nitonfx.signaling;

import eu.nitonfx.signaling.api.Context;
import eu.nitonfx.signaling.api.ListSignal;
import eu.nitonfx.signaling.api.Signal;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
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
    public T remove(int index) {
        final var removed = super.remove(index);
        size.set(list.size());
        return removed;
    }
    @Override
    public int size() {
        return size.get();
    }

    @Override
    public Signal<T> getSignal(int index) {
        return list.get(index);
    }
}
