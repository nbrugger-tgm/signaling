package eu.nitonfx.signaling.api;

import eu.nitonfx.signaling.SetStackContext;

import java.util.Collections;
import java.util.List;

public interface Context {
    Context global = new SetStackContext();

    static Context create() {
        return new SetStackContext();
    }

    <T> Signal<T> createSignal(T initial);

    <T> ListSignal<T> createSignal(List<T> initial);
    <T> ListSignal<T> createSignal(T[] initial);

    void createEffect(Runnable effect);

    void cleanup(Runnable o);
}
