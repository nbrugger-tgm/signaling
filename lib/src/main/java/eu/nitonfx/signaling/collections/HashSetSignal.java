package eu.nitonfx.signaling.collections;

import eu.nitonfx.signaling.Effect;
import eu.nitonfx.signaling.api.*;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class HashSetSignal<E> extends AbstractSet<E> implements SetSignal<E> {
    private final Set<E> set;
    private final Context cx;
    private final Signal<Integer> size;
    private final List<Reconciler<E>> reconcilers = new ArrayList<>();
    private interface Reconciler<T> {
        void onAdd(T element);
        void onRemove(T element);
    }

    public HashSetSignal(Context cx) {
        this(cx, Set.of());
    }

    private void onRemove(E signal) {
        reconcilers.forEach(it -> it.onRemove(signal));
    }

    private void onAdd(E signal) {
        reconcilers.forEach(it -> it.onAdd(signal));
    }

    public HashSetSignal(Context cx, Set<E> initial) {
        this.cx = cx;
        set = new ListenableSet<>(
                new HashSet<>(initial),
                this::onAdd, this::onRemove
        );
        size = cx.createSignal(initial.size());
    }

    @Override
    public void clear() {
        set.clear();
        size.set(0);
    }

    @Override
    public boolean remove(Object o) {
        var removed = set.remove(o);
        if (removed)
            size.set(set.size());
        return removed;
    }

    @Override
    public boolean add(E e) {
        var changed = set.add(e);
        if (changed)
            size.set(set.size());
        return changed;
    }

    @Override
    public Iterator<E> iterator() {
        size.get();//this is to make sure the size signal is subscribed to
        return set.iterator();
    }

    @Override
    public int size() {
        return size.get();
    }

    @Override
    @Unmodifiable
    public Set<E> getUntracked() {
        return set;
    }

    @Override
    public EffectHandle onAdd(Consumer<E> consumer) {
        record ElementEffectHandle(EffectHandle handle, Object element) {}
        List<ElementEffectHandle> handles = new ArrayList<>();
        for (var signal : set) {
            var handle = cx.createEffect(() -> consumer.accept(signal));
            handles.add(new ElementEffectHandle(handle, signal));
        }
        var reconciler = new Reconciler<E>() {
            @Override
            public void onAdd(E signal) {
                var handle = cx.createEffect(() -> consumer.accept(signal));
                handles.add(new ElementEffectHandle(handle, signal));
            }

            @Override
            public void onRemove(E index) {
                var element = handles.stream()
                        .filter(handle -> handle.element.equals(index))
                        .findFirst()
                        .orElseThrow();
                element.handle.cancel();
                handles.remove(element);
            }
        };
        reconcilers.add(reconciler);
        return EffectHandle.of("SetOnAddEffect",()->{
        final var effect = EffectHandle.of("SetOnAddEffect", () -> {
            reconcilers.remove(reconciler);
            handles.forEach(e -> e.handle.cancel());
            handles.clear();
        },()->handles.stream().map(it -> "|-"+it).collect(Collectors.joining("\n")));
        }, () -> handles.stream().map(it -> "|-" + it).collect(Collectors.joining("\n")));
        cx.registerEffect(effect);
        return effect;
    }
    }
}
