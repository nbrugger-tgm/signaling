package eu.nitonfx.signaling;

import eu.nitonfx.signaling.api.Context;
import eu.nitonfx.signaling.api.SetSignal;
import eu.nitonfx.signaling.api.Signal;
import eu.nitonfx.signaling.api.SignalLike;
import org.jetbrains.annotations.Unmodifiable;

import java.util.AbstractSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

public class HashSetSignal<E> extends AbstractSet<E> implements SetSignal<E> {
    private final Set<Signal<E>> set = new HashSet<>();
    private final Context cx;
    private final Signal<Integer> size;

    public HashSetSignal(Context cx) {
        this.cx = cx;
        size = cx.createSignal(0);
    }

    public HashSetSignal(Context cx, Set<E> initial) {
        this.cx = cx;
        initial.stream().map(cx::createSignal).forEach(set::add);
        size = cx.createSignal(initial.size());
    }

    @Override
    public void clear() {
        set.clear();
        size.set(0);
    }

    @Override
    public boolean remove(Object o) {
        var removed = set.remove(cx.createSignal((E) o));
        if (removed)
            size.set(set.size());
        return removed;
    }

    @Override
    public boolean add(E e) {
        var changed = set.add(cx.createSignal(e));
        if (changed)
            size.set(set.size());
        return changed;
    }

    @Override
    public Iterator<E> iterator() {
        var signalingIter = set.iterator();
        size.get();//this is to make sure the size signal is subscribed to
        return new Iterator<E>() {
            @Override
            public boolean hasNext() {
                return signalingIter.hasNext();
            }

            @Override
            public E next() {
                return signalingIter.next().get();
            }

            @Override
            public void remove() {
                signalingIter.remove();
                size.set(set.size());
            }
        };
    }

    @Override
    public int size() {
        return size.get();
    }

    @Override
    @Unmodifiable
    public Set<E> getUntracked() {
        return set.stream().map(SignalLike::getUntracked).collect(Collectors.toSet());
    }
}
