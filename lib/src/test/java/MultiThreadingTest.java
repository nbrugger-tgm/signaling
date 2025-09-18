import eu.nitonfx.signaling.api.Context;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;
import java.util.stream.IntStream;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class MultiThreadingTest {
    @Test
    void asyncWrite() throws InterruptedException {
        var cx = Context.create();
        var count = cx.createSignal(0);

        Consumer<Integer> consumer = mock();
        cx.createEffect(() -> {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
            }
            consumer.accept(count.get());
        });
        long start = System.currentTimeMillis();
        IntStream.range(0, 5).mapToObj(number -> new Thread(() -> count.set(number))).peek(Thread::start).forEach(thread -> {
            try {
                thread.join();
            } catch (InterruptedException ignored) {
            }
        });
        long end = System.currentTimeMillis();
        System.out.println("Time: " + (end - start)+" ms");
        verify(consumer).accept(0);
        verify(consumer).accept(0);
        verify(consumer).accept(1);
        verify(consumer).accept(2);
        verify(consumer).accept(3);
        verify(consumer).accept(4);
    }
}
