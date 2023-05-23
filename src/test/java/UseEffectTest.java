import eu.nitonfx.signaling.api.Context;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.mockito.Mockito.*;

public class UseEffectTest {
    @Test
    void shoudCallUseEffectOnChange() {
        Consumer<Integer> consumer = mock();
        Context cx = Context.create();
        var count = cx.createSignal(0);
        count.set(5);
        cx.createEffect(() -> consumer.accept(count.get()));
        count.set(12);
        verify(consumer).accept(5);
        verify(consumer).accept(12);
    }

    @Test
    void useCase() {
        Consumer<Integer> consumer = mock();
        var cx = Context.create();
        var count = cx.createSignal(0);
        var doubleCount = count.derive(i -> i * 2);
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

    @Test
    void useCase2() {
        Consumer<Integer> consumer = mock();
        var cx = Context.create();
        var count = cx.createSignal(0);
        var doubleCount = count.derive(i -> i * 2);
        cx.createEffect(() -> {
            cx.createEffect(() -> consumer.accept(doubleCount.get()));
            cx.createEffect(() -> consumer.accept(count.get()));
            consumer.accept(count.get() * count.get());
        });
        count.set(5);
        count.set(12);
        verify(consumer, times(3)).accept(0);

        verify(consumer).accept(10);
        verify(consumer).accept(5);
        verify(consumer).accept(25);

        verify(consumer).accept(24);
        verify(consumer).accept(12);
        verify(consumer).accept(144);
    }


    @Test
    void nestedEffectOnlyReactsToNonParentChanges() {
        Consumer<Integer> consumer = mock();
        var cx = Context.create();
        var count = cx.createSignal(0);
        var count2 = cx.createSignal(0);
        cx.createEffect(() -> {
            count2.get();
            cx.createEffect(() -> consumer.accept(count2.get()+count.get()));
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
        var cx = Context.create();
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
        var cx = Context.create();
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
        var cx = Context.create();
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
        var cx = Context.create();
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
        var cx = Context.create();
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
        var cx = Context.create();
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
    void recursiveEffect(){
        var cx = Context.create();
        var count = cx.createSignal(0);

        Consumer<Integer> target = mock(Consumer.class);

        cx.createEffect(() -> {
            target.accept(count.get());
            if(count.get() < 5){
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
}
