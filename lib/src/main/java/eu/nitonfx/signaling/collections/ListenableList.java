package eu.nitonfx.signaling.collections;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.AbstractList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntFunction;

public class ListenableList<T> extends AbstractList<T> {
    private final List<T> backing;
    @Nullable private final Consumer<Integer> removeCallback;
    @Nullable private final BiConsumer<T,Integer> addCallback;
    @Nullable private final BiConsumer<T, Integer> setCallback;

    public ListenableList(
            List<T> backing,
            @Nullable Consumer<Integer> removeCallback,
            @Nullable BiConsumer<T, Integer> addCallback,
            @Nullable BiConsumer<T, Integer> setCallback
    ) {
        this.backing = backing;
        this.removeCallback = removeCallback;
        this.addCallback = addCallback;
        this.setCallback = setCallback;
    }

    @Override
    public T get(int i) {
        return backing.get(i);
    }

    @Override
    public T set(int index, T element) {
        final var set = backing.set(index, element);
        if(setCallback != null) setCallback.accept(set, index);
        return set;
    }

    @Override
    public void add(int index, T element) {
        backing.add(index, element);
        if(addCallback != null) addCallback.accept(element, index);
    }

    @Override
    public T remove(int index) {
        final var remove = backing.remove(index);
        if(removeCallback != null) removeCallback.accept(index);
        return remove;
    }

    @Override
    public T getFirst() {
        return backing.getFirst();
    }

    @Override
    public T getLast() {
        return backing.getLast();
    }

    @Override
    public int size() {
        return backing.size();
    }

    @Override
    public int indexOf(Object o) {
        return backing.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return backing.lastIndexOf(o);
    }

    @Override
    public boolean isEmpty() {
        return backing.isEmpty();
    }

    @Override
    public @NotNull Object[] toArray() {
        return backing.toArray();
    }

    @Override
    public @NotNull <T1> T1[] toArray(@NotNull T1[] a) {
        return backing.toArray(a);
    }

    @Override
    public <T1> T1[] toArray(@NotNull IntFunction<T1[]> generator) {
        return backing.toArray(generator);
    }

    @Override
    public boolean contains(Object o) {
        return backing.contains(o);
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        return backing.containsAll(c);
    }
}
