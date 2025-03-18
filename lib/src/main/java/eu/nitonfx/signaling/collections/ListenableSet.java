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
        var ret = super.remove(o);
        if(ret) removeListener.accept((E) o);
        return ret;
    }

    @Override
    public Iterator<E> iterator() {
        return set.iterator();
    }

    @Override
    public int size() {
        return set.size();
    }
}
