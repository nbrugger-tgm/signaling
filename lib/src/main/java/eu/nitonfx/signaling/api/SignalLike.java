package eu.nitonfx.signaling.api;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A signal is a value that can be subscribed to
 *
 * @param <T> the type of the value
 */
public interface SignalLike<T> extends Supplier<T> {

    /**
     * Returns the current value of the signal.
     * This method tracks dependencies and should be used within reactive contexts.
     *
     * @return the current value of the signal
     */
    T get();

    /**
     * Returns the current value of the signal without tracking dependencies.
     * This method should be used when you want to access the value without triggering reactivity.
     *
     * @return the current value of the signal without tracking dependencies
     */
    T getUntracked();

    /**
     * Listen to this signal getting dirty, allowing dependency tracking, delayed execution;
     * this is the preferred mode of listening.
     * {@link #propagateDirty(Consumer)} is an escape hatch to act on dirty immediately without dependency tracking support.
     *
     * @param consumer a listener which is called when the signal might need to be read again because the value might have changed,
     * @return an object that can be used to cancel the subscription
     */
    Subscription onDirtyEffect(Consumer<SignalLike<T>> consumer);

    /**
     * Listen to the signal getting dirty in real time without delays.
     * This means that delayed execution dependency tracking and deferred execution do not work,
     * using any {@link Context} methods in the callback/listener will cause issues and undefined behavior.
     *
     * @param consumer a method being called when the signal needs to be read again to make sure its value (didn't) changed.
     * @return an object that can be used to cancel the subscription
     */
    Subscription propagateDirty(Consumer<SignalLike<T>> consumer);

    default <N> SignalLike<N> map(Function<T, N> mapper) {
        class View<I> implements SignalLike<I>{
            private final Function<T, I> mapper;

            View(Function<T, I> mapper) {
                this.mapper = mapper;
            }

            @Override
            public I get() {
                return mapper.apply(SignalLike.this.get());
            }

            @Override
            public I getUntracked() {
                return mapper.apply(SignalLike.this.get());
            }

            @Override
            public Subscription onDirtyEffect(Consumer<SignalLike<I>> consumer) {
                return SignalLike.this.onDirtyEffect(signal -> consumer.accept(this));
            }

            @Override
            public Subscription propagateDirty(Consumer<SignalLike<I>> consumer) {
                return SignalLike.this.propagateDirty(signal -> consumer.accept(this));
            }
        }
        return new View<>(mapper);
    }

}
