import eu.nitonfx.signaling.api.Context;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class MultiThreaded {
    @Test
    void asyncWrite() throws InterruptedException {
        var cx = Context.create();
        var count = cx.createSignal(0);

        Consumer<Integer> consumer = mock();
        cx.createEffect(() -> {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {}
            consumer.accept(count.get());
        });
        ThreadGroup group = new ThreadGroup("concurrent writes");
        for (int i = 0; i < 5; i++) {
            int finalI = i;
            new Thread(group, ()->count.set(finalI)).start();
        }
        Thread.sleep(50);
        verify(consumer).accept(0);
        verify(consumer).accept(0);
        verify(consumer).accept(1);
        verify(consumer).accept(2);
        verify(consumer).accept(3);
        verify(consumer).accept(4);
    }
}
