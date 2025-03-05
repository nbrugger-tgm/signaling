package eu.nitonfx.signaling.api;

import eu.nitonfx.signaling.SetStackContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * The Context interface provides methods to create and manage reactive signals, effects, and memoized values.
 * It allows for the creation of signals, which are values that can be subscribed to and updated.
 * It also provides methods to create effects, which are functions that run when their dependencies change,
 * and memoized values, which are values that are computed once and cached until their dependencies change.
 * 
 * A context is a container for reactive state and computations. It is needed to manage the lifecycle of reactive computations
 * and to ensure that they are properly disposed of when no longer needed. Different contexts do not interact with each other,
 * meaning that signals and effects created in one context are isolated from those created in another context.
 */
public interface Context {
    Context global = new SetStackContext();

    /**
     * Creates a new Context instance.
     *
     * @return a new Context instance
     */
    static Context create() {
        return new SetStackContext();
    }

    /**
     * Creates a new signal with the given initial value.
     *
     * @param initial the initial value of the signal
     * @param <T>     the type of the value
     * @return a new Signal instance
     */
    <T> Signal<T> createSignal(T initial);

    /**
     * Creates a new signal with a null initial value.
     *
     * @param <T> the type of the value
     * @return a new Signal instance with a null initial value
     */
    <T> Signal<T> createNullSignal();

    /**
     * Creates a new ListSignal with the given initial values.
     *
     * @param initial the initial values of the ListSignal
     * @param <T>     the type of the values
     * @return a new ListSignal instance
     */
    <T> ListSignal<T> createSignal(List<T> initial);

    /**
     * Creates a new ListSignal with the given initial values.
     *
     * @param initial the initial values of the ListSignal
     * @param <T>     the type of the values
     * @return a new ListSignal instance
     */
    <T> ListSignal<T> createSignal(T[] initial);

    /**
     * Creates a new SetSignal with the given initial values.
     *
     * @param initial the initial values of the SetSignal
     * @param <T>     the type of the values
     * @return a new SetSignal instance
     */
    <T> SetSignal<T> createSignal(Set<T> initial);

    /**
     * Creates a new MapSignal with the given initial values.
     *
     * @param initial the initial values of the MapSignal
     * @param <K>     the type of the keys
     * @param <V>     the type of the values
     * @return a new MapSignal instance
     */
    <K, V> MapSignal<K, V> createSignal(Map<K, V> initial);

    /**
     * Creates a memoized value that is computed using the given function.
     * The value is cached until one of its dependencies changes.
     * The memoized value is computed lazily, meaning it is only computed when it is first accessed.
     * The memoized value uses a pull-based approach, meaning it is updated when its subscribers request the value.
     *
     * @param function the function to compute the value
     * @param <T>      the type of the value
     * @return a Supplier that provides the memoized value
     */
    <T> Supplier<T> createMemo(@NotNull Supplier<T> function);

    /**
     * Executes the given function without tracking its dependencies.
     *
     * @param function the function to execute
     * @param <T>      the type of the value returned by the function
     * @return the value returned by the function
     */
    <T> T untracked(@NotNull Supplier<T> function);

    /**
     * Creates an effect that runs the given function whenever one of its dependencies changes.
     *
     * @param effect the function to run
     * @return an EffectHandle that can be used to manually cancel the effect
     */
    EffectHandle createEffect(Runnable effect);

    /**
     * Registers a cleanup function to be called when the current effect is disposed.
     * This happens when an effect is re-run. This should be used to revert all side-effects a effect has.
     * For example if an effect inserts an UI element into a container, the cleanup should remove said element
     *
     * @param o the cleanup function
     */
    void cleanup(Runnable o);

    /**
     * Executes the given effect without tracking its dependencies.
     *
     * @param effect the effect to execute
     */
    void untracked(Runnable effect);

    /**
     * Runs the given effect immediately.
     *
     * @param effect the effect to run
     */
    void run(Runnable effect);

    /**
     * @return an empty list signal
     * @param <T> the element type of the list {@link  ListSignal}
     */
    <T> ListSignal<T> createListSignal();
}
