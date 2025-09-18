package eu.nitonfx.signaling.collections;

import org.jetbrains.annotations.NotNull;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Consumer;

public class ListenableSet<E> extends AbstractSet<E> {
    private final Set<E> set;
    private final Consumer<? super E> addListener;
    private final Consumer<? super E> removeListener;

    public ListenableSet(Set<E> set, Consumer<? super E> addListener, Consumer<? super E> removeListener) {
        this.set = set;
        this.addListener = addListener;
        this.removeListener = removeListener;
    }

    @Override
    public boolean add(E e) {
        var ret = set.add(e);
        if(ret) addListener.accept(e);
        return ret;
    }

    @Override
    public boolean remove(Object o) {
        var ret = set.remove(o);
        if(ret) removeListener.accept((E) o);
        return ret;
    }

    @Override
    public @NotNull Iterator<E> iterator() {
        var innerIter = set.iterator();
        return new Iterator<E>() {
            E last;
            @Override
            public boolean hasNext() {
                return innerIter.hasNext();
            }

            @Override
            public E next() {
                last = innerIter.next();
                return last;
            }

            @Override
            public void remove() {
                innerIter.remove();
                removeListener.accept(last);
            }
        };
    }

    @Override
    public int size() {
        return set.size();
    }
}
