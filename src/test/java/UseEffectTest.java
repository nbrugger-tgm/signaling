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
        verify(consumer,times(3)).accept(0);

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
        verify(consumer,times(3)).accept(0);

        verify(consumer).accept(10);
        verify(consumer).accept(5);
        verify(consumer).accept(25);

        verify(consumer).accept(24);
        verify(consumer).accept(12);
        verify(consumer).accept(144);
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
}
