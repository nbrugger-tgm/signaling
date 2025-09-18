package eu.nitonfx.signaling.api;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.ArrayList;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.*;

public abstract class UseEffectTest {
    protected abstract Context createContext();
    @Test
    void shoudCallUseEffectOnChange() {
        Consumer<Integer> consumer = mock();
        Context cx = createContext();
        var count = cx.createSignal(0);
        count.set(5);
        cx.createEffect(() -> consumer.accept(count.get()));
        count.set(12);
        verify(consumer).accept(5);
        verify(consumer).accept(12);
    }

    @RepeatedTest(15)
    void useCase() {
        Consumer<Integer> consumer = mock();
        var cx = createContext();
        var count = cx.createSignal(0);
        var doubleCount = cx.createMemo(() -> count.get() * 2);
        cx.createEffect(() -> {
            consumer.accept(count.get() * count.get());
            cx.createEffect(() -> consumer.accept(doubleCount.get()));
            cx.createEffect(() -> consumer.accept(count.get()));
        });
        count.set(5);
        count.set(12);
        verify(consumer, times(3)).accept(0);

        verify(consumer).accept(25);
        verify(consumer).accept(10);
        verify(consumer).accept(5);

        verify(consumer).accept(144);
        verify(consumer).accept(24);
        verify(consumer).accept(12);
    }

    @RepeatedTest(15)
    void useCase2() {
        Consumer<Integer> consumer = mock();
        var cx = createContext();
        cx.run(()->{
            var count = cx.createSignal(0);
            var doubleCount = cx.createMemo(() -> count.get() * 2);

            cx.createEffect(() -> {
                cx.createEffect(() -> consumer.accept(doubleCount.get()));
                cx.createEffect(() -> consumer.accept(count.get()));
                consumer.accept(count.get() * count.get());
            });
            count.set(5);
            count.set(12);
        });

        verify(consumer).accept(144);
        verify(consumer).accept(24);
        verify(consumer).accept(12);
        verifyNoMoreInteractions(consumer);
    }


    @Test
    void nestedEffectOnlyReactsToNonParentChanges() {
        Consumer<Integer> consumer = mock();
        var cx = createContext();
        var count = cx.createSignal(0);
        var count2 = cx.createSignal(0);
        cx.createEffect(() -> {
            count2.get();
            cx.createEffect(() -> consumer.accept(count2.get() + count.get()));
        });
        count.set(5);
        count2.set(10);
        verify(consumer).accept(0);
        verify(consumer).accept(5);
        verify(consumer).accept(15);
    }

    @Test
    void cascase() {
        Consumer<Integer> target = mock();
        var cx = createContext();
        var count = cx.createSignal(0);
        var count2 = cx.createSignal(0);
        cx.createEffect(() -> count2.set(count.get()));
        cx.createEffect(() -> target.accept(count2.get()));
        count.set(5);
        count.set(12);
        verify(target).accept(5);
        verify(target).accept(12);
    }

    @Test
    void nestedCascade() {
        Consumer<Integer> target = mock();
        var cx = createContext();
        var count = cx.createSignal(0);
        var count2 = cx.createSignal(0);
        cx.createEffect(() -> count2.set(count.get()));
        cx.createEffect(() -> {
            count2.get();
            cx.createEffect(() -> target.accept(count2.get()));
        });
        count.set(5);
        count.set(12);
        verify(target).accept(5);
        verify(target).accept(12);
    }

    @Test
    void onlyRequiredEffectsAreRun() {
        Consumer<Integer> target = mock();
        Consumer<Integer> target2 = mock();
        var cx = createContext();
        var count = cx.createSignal(0);
        var count2 = cx.createSignal(1);

        cx.createEffect(() -> {
            target.accept(count.get());
            cx.createEffect(() -> target2.accept(count2.get()));
        });

        verify(target).accept(0);
        verify(target2).accept(1);
        reset(target, target2);

        count2.set(5);
        verify(target, never()).accept(anyInt());
        verify(target2).accept(5);
        reset(target, target2);

        count.set(3);
        verify(target).accept(3);
        verify(target2).accept(5);
    }

    @Test
    void conditionalReactivity() {
        var cx = createContext();
        var loggedIn = cx.createSignal(false);
        var secretValue = cx.createSignal("s3cr3t");

        Consumer<String> target = mock(Consumer.class);
        var generalRunnable = mock(Runnable.class);
        cx.createEffect(() -> {
            if (loggedIn.get()) {
                target.accept(secretValue.get());
            }
            generalRunnable.run();
        });

        //setting the secret value should not trigger the target since it wasn't used in the last run due to loggedIn being false
        verify(target, never()).accept(anyString());
        secretValue.set("new secret");
        verify(target, never()).accept(anyString());
        verify(generalRunnable).run(); //ran once for initialization


        loggedIn.set(true);
        verify(target).accept("new secret");
        verify(generalRunnable, times(2)).run();

        //now that loggedIn is true, secretValue changes should trigger the effect
        secretValue.set("new secret 2");
        verify(target).accept("new secret 2");
        verify(generalRunnable, times(3)).run();
    }

    @Test
    void conditionalNesting() {
        var cx = createContext();
        var loggedIn = cx.createSignal(false);
        var secretValue = cx.createSignal("s3cr3t");

        Consumer<String> target = mock(Consumer.class);
        var generalRunnable = mock(Runnable.class);
        cx.createEffect(() -> {
            if (loggedIn.get()) {
                cx.createEffect(() -> target.accept(secretValue.get()));
            }
            generalRunnable.run();
        });

        //setting the secret value should not trigger the target since it wasn't used in the last run due to loggedIn being false
        verify(target, never()).accept(anyString());
        secretValue.set("new secret");
        verify(target, never()).accept(anyString());
        verify(generalRunnable).run(); //ran once for initialization


        loggedIn.set(true);
        verify(target).accept("new secret");
        verify(generalRunnable, times(2)).run();

        //now that loggedIn is true, secretValue changes should trigger the effect
        secretValue.set("new secret 2");
        verify(target).accept("new secret 2");
        //only 2 times because the outer effect is not subscribed to secretValue but only the nested one
        verify(generalRunnable, times(2)).run();
    }

    @Test
    void chainingWorks() {
        var cx = createContext();
        var count = cx.createSignal(0);
        var count2 = cx.createSignal(0);
        var count3 = cx.createSignal(0);
        var count4 = cx.createSignal(0);

        Consumer<Integer> target = mock(Consumer.class);

        cx.createEffect(() -> count2.set(count.get()));
        cx.createEffect(() -> count3.set(count2.get()));
        cx.createEffect(() -> count4.set(count3.get()));
        cx.createEffect(() -> target.accept(count4.get()));

        verify(target).accept(0);
        reset(target);

        count.set(5);
        verify(target).accept(5);
    }

    @Test
    void recursiveEffect() {
        var cx = createContext();
        var count = cx.createSignal(0);

        Consumer<Integer> target = mock(Consumer.class);

        cx.createEffect(() -> {
            target.accept(count.get());
            if (count.get() < 5) {
                count.update(e -> e + 1);
            }
        });

        verify(target).accept(0);
        verify(target).accept(1);
        verify(target).accept(2);
        verify(target).accept(3);
        verify(target).accept(4);
        verify(target).accept(5);
    }

    @Test
    void nestedEffectsAreCleaned() {
        var cx = createContext();
        var count = cx.createSignal(0);
        var count2 = cx.createSignal(0);
        Consumer<Integer> consumer = mock();
        cx.createEffect(() -> {
            count.get();
            cx.createEffect(() -> consumer.accept(count2.get()));
        });
        count.update(e -> e + 1);
        count.update(e -> e + 1);
        count2.set(5);
        verify(consumer, times(3)).accept(0);
        verify(consumer).accept(5);
    }

    @RepeatedTest(10)
    void executionOrder(){
        var cx = createContext();
        var rootCount = cx.createSignal(0);
        var count1 = cx.createSignal(0);
        var count2 = cx.createSignal(0);
        var count3 = cx.createSignal(0);
        var count4 = cx.createSignal(0);
        var count5 = cx.createSignal(0);
        var list = new ArrayList<>(5);

        cx.createEffect(() -> {
            if(count5.get() == 5) {
                list.add(5);
            }
        });
        cx.createEffect(() -> {
            if(count4.get() == 5) {
                list.add(4);
            }
        });
        cx.createEffect(() -> {
            if(count3.get() == 5) {
                list.add(3);
            }
            count4.set(count3.get());
            cx.createEffect(()-> count5.set(count3.get()));
        });
        cx.createEffect(() -> {
            if(count2.get() == 5) {
                list.add(2);
            }
        });
        cx.createEffect(() -> {
            if(count1.get() == 5) {
                list.add(1);
            }
        });
        cx.createEffect(() -> {
            cx.createEffect(() -> count1.set(rootCount.get()));
            count2.set(rootCount.get());
            count3.set(rootCount.get());
        });
        rootCount.set(5);
        assertThat(list).containsExactly(1,2,3,5,4);
    }

    @Test
    void cleanupRunsBeforeNewExecution(){
        var cx = createContext();
        var count = cx.createSignal(0);
        Consumer<Integer> cleanup = mock();
        Consumer<Integer> effect = mock();
        cx.createEffect(() -> {
            effect.accept(count.get());
            cx.cleanup(()->cleanup.accept(count.getUntracked()));
        });
        count.set(5);
        count.set(6);
        InOrder inOrder = inOrder(cleanup, effect);
        inOrder.verify(effect).accept(0);
        inOrder.verify(cleanup).accept(5);
        inOrder.verify(effect).accept(5);
        inOrder.verify(cleanup).accept(6);
        inOrder.verify(effect).accept(6);
    }

    @Test
    void cleanupRunsOnParentExecution(){
        var cx = createContext();
        var count = cx.createSignal(0);
        Consumer<Integer> cleanup = mock();
        Consumer<Integer> effect = mock();
        cx.createEffect(()->{
            effect.accept(count.get());
            cx.createEffect(()->{
                cx.cleanup(()->cleanup.accept(count.getUntracked()));
            });
        });
        count.set(3);
        InOrder inOrder = inOrder(cleanup, effect);
        inOrder.verify(effect).accept(0);//parent
        inOrder.verify(cleanup).accept(3);
        inOrder.verify(effect).accept(3);
    }

    @Test
    void innerSignalDoesntTriggerOuterEffect(){
        var cx = createContext();
        cx.createEffect(() -> {
            var count = cx.createSignal(0);
            count.set(5);
            assertThatExceptionOfType(UnsupportedOperationException.class)
                    .as("signals cannot be read in the effect that created them")
                    .isThrownBy(count::get)
            ;
        });
    }

    @Test
    void manualEffectCancellation() {
        var cx = createContext();
        var count = cx.createSignal(0);
        Consumer<Integer> effect = mock();
        var effectHandle = cx.createEffect(() -> effect.accept(count.get()));
        count.set(5);
        verify(effect).accept(0);
        verify(effect).accept(5);
        effectHandle.cancel();
        count.set(10);
        verify(effect, never()).accept(10);
    }
}
