package eu.nitonfx.signaling.api;

import eu.nitonfx.signaling.SetStackContext;

public interface Context {
    Context global = new SetStackContext();

    static Context create() {
        return new SetStackContext();
    }

    <T> Signal<T> createSignal(T initial);

    void createEffect(Runnable effect);

    void cleanup(Runnable o);
}
