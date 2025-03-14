package eu.nitonfx.signaling;

import eu.nitonfx.signaling.api.*;
import eu.nitonfx.signaling.collections.ArraySignalList;
import eu.nitonfx.signaling.collections.HashSetSignal;
import eu.nitonfx.signaling.collections.SetMapSignal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;

public class SetStackContext implements Context {
    private final Set<Dependency<?>> dependencies = new HashSet<>();
    private final List<Effect> nestedEffects = new ArrayList<>(3);
    private final List<Supplier<Set<Runnable>>> deferredEffects = new ArrayList<>(8);
    private final Set<Runnable> cleanup = new HashSet<>();
    @Nullable
    private Runnable recording = null;

    /**
     * @param <T> The type of the signal, may not be a specific implementation of a container (ArrayList, HashSet, etc.)
     * @throws ClassCastException when {@code <T>} is a specific implementation of a container
     */
    @Override
    public <T> Signal<T> createSignal(T initial) {
        return createObjectSignal(initial);
    }

    private <T> Signal<T> createObjectSignal(T initial) {
        T container = switch (initial) {
            case null -> null;
            case List<?> list -> (T) createSignal(list);
            case Set<?> set -> (T) new HashSetSignal<>(this, set);
            default -> initial;
        };
        var creationEffect = recording;
        return new MutableSignal<>(
                (subscribable) -> onSignalRead(subscribable, creationEffect),
                this::onSignalWrite,
                container
        );
    }

    private void onSignalWrite(Supplier<Set<Runnable>> observers) {
        if (recording != null) deferredEffects.add(observers);
        else run(()-> {
            for (Runnable runnable : observers.get()) {
                runnable.run();
            }
        });
    }

    /**
     * @param creationEffect the effect that the signal was created in
     */
    private <T> void onSignalRead(SignalLike<T> subscribable, Runnable creationEffect) {
        if (recording == null || recording == creationEffect)
            return;
        //remove outdated signal dependencies
        //a signal can be read twice in an effect and with the value changing in between reads
        //in such a case only the last read value counts
        dependencies.removeIf(dep -> dep.signal() == subscribable);

        dependencies.add(new Dependency<>(subscribable));
    }

    @Override
    public <T> Signal<@Nullable T> createNullSignal() {
        return createObjectSignal(null);
    }

    @Override
    public <T> ListSignal<T> createSignal(List<T> initial) {
        return new ArraySignalList<>(this, initial, getParentStackElement());
    }

    @Override
    public <T> ListSignal<T> createSignal(T[] initial) {
        return new ArraySignalList<>(this, Arrays.stream(initial).toList(), getParentStackElement());
    }

    @Override
    public <T> SetSignal<T> createSignal(Set<T> initial) {
        return new HashSetSignal<>(this, initial);
    }

    @Override
    public <K, V> MapSignal<K, V> createSignal(Map<K, V> initial) {
        return new SetMapSignal<>(this, initial, getParentStackElement());
    }

    @Override
    public <T> Supplier<T> createMemo(@NotNull Supplier<T> function) {
        var creationEffect = recording;
        return new DerivedSignal<>(
                getParentStackElement(),
                function,
                (signal) -> this.onSignalRead(signal, creationEffect),
                this::runAndCaptureInIsolation,
                this::onSignalWrite
        );
    }

    private EffectCapture runAndCaptureInIsolation(Runnable runnable) {
        var disabledRecording = recording;
        var disabledDependencies = new HashSet<>(dependencies);
        var disabledNestedEffects = new ArrayList<>(nestedEffects);
        var disabledDeferredSignalUpdates = new ArrayList<>(deferredEffects);
        var disabledClanups = new HashSet<>(cleanup);

        dependencies.clear();
        nestedEffects.clear();
        deferredEffects.clear();
        cleanup.clear();

        recording = runnable;
        runnable.run();
        recording = disabledRecording;
        var isolatedRecording = new EffectCapture(
                Set.copyOf(dependencies),
                List.copyOf(nestedEffects),
                List.copyOf(deferredEffects),
                Set.copyOf(cleanup)
        );

        dependencies.clear();
        nestedEffects.clear();
        deferredEffects.clear();
        cleanup.clear();
        dependencies.addAll(disabledDependencies);
        nestedEffects.addAll(disabledNestedEffects);
        deferredEffects.addAll(disabledDeferredSignalUpdates);
        cleanup.addAll(disabledClanups);
        return isolatedRecording;
    }

    @Override
    public <T> T untracked(@NotNull Supplier<T> function) {
        var disabledRecording = recording;
        var disabledDependencies = new HashSet<>(dependencies);
        var disabledNestedEffects = new ArrayList<>(nestedEffects);
        var disabledDeferredSignalUpdates = new ArrayList<>(deferredEffects);
        var disabledClanups = new HashSet<>(cleanup);
        recording = null;
        T val = function.get();
        recording = disabledRecording;
        dependencies.clear();
        nestedEffects.clear();
        deferredEffects.clear();
        cleanup.clear();
        dependencies.addAll(disabledDependencies);
        nestedEffects.addAll(disabledNestedEffects);
        deferredEffects.addAll(disabledDeferredSignalUpdates);
        cleanup.addAll(disabledClanups);
        return val;
    }

    private StackTraceElement getParentStackElement() {
        return Thread.currentThread().getStackTrace()[2];
    }

    @Override
    public synchronized EffectHandle createEffect(Runnable effect) {
        var effectWrapper = new Effect(effect, this::runAndCapture, this::runEffect);
        runEffect(effectWrapper);
        return effectWrapper;
    }

    private void runEffect(Effect effectWrapper) {
        if (recording != null) nestedEffects.add(effectWrapper);
        else effectWrapper.run();
    }

    @Override
    public synchronized void cleanup(Runnable func) {
        if (recording == null)
            throw new IllegalStateException("Cleanup was called outside of an effect!");
        cleanup.add(func);
    }

    @Override
    public synchronized void untracked(Runnable effect) {
       untracked(() -> {
           effect.run();
           return null;
       });
    }

    private synchronized EffectCapture runAndCapture(Runnable effect) {
        dependencies.clear();
        nestedEffects.clear();
        deferredEffects.clear();
        cleanup.clear();
        recording = effect;
        effect.run();
        recording = null;
        return new EffectCapture(Set.copyOf(dependencies), List.copyOf(nestedEffects), List.copyOf(deferredEffects), Set.copyOf(cleanup));
    }

    @Override
    public void run(Runnable effect) {
        var capture = runAndCapture(effect);
        capture.nestedEffects().forEach(Effect::run);
        capture.flatDeferredEffects().forEach(this::run);
        if (!capture.cleanup().isEmpty()) throw new IllegalStateException("Cleanup was called outside of an effect!");
    }

    @Override
    public <T> ListSignal<T> createListSignal() {
        return new ArraySignalList<>(this, getParentStackElement());
    }
}
