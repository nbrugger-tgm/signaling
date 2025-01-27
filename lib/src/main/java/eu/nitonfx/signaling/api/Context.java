package eu.nitonfx.signaling.api;

import eu.nitonfx.signaling.SetStackContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public interface Context {
    Context global = new SetStackContext();

    static Context create() {
        return new SetStackContext();
    }

    <T> Signal<T> createSignal(T initial);
    <T> Signal<T> createNullSignal();

    <T> ListSignal<T> createSignal(List<T> initial);
    <T> ListSignal<T> createSignal(T[] initial);
    <T> SetSignal<T> createSignal(Set<T> initial);
    <K,V> MapSignal<K,V> createSignal(Map<K,V> initial);

    <T> Supplier<T> createMemo(@NotNull Supplier<T> function);

    <T> T untracked(@NotNull Supplier<T> function);

    void createEffect(Runnable effect);

    void cleanup(Runnable o);

    void untracked(Runnable effect);

    void run(Runnable effect);
}
