import eu.nitonfx.signaling.api.Context;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Test
    void asyncWriteDuringEffect() {
        var cx = Context.create();
        CountDownLatch effectTrigger = new CountDownLatch(1);
        CountDownLatch asyncTrigger = new CountDownLatch(1);
        var count = cx.createSignal(0);
        var effectCalledCounter = cx.createSignal(0);
        var unrelatedSignal = cx.createSignal(1);
        cx.createEffect(() -> {
            try {
                if(count.get() == 1) effectTrigger.await();
            } catch (InterruptedException e) {
            }
            effectCalledCounter.update(it -> it+1);
        });
        assertThat(effectCalledCounter.getUntracked()).isEqualTo(1);
        CompletableFuture.runAsync(()->{
            try {
                asyncTrigger.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            var x = unrelatedSignal.get();
        });
        CompletableFuture.runAsync(()->count.set(1));//triggers effect
        //effect runs now and is halted
        asyncTrigger.countDown();//cause "external thread" to write a signal
        effectTrigger.countDown();//let the effect end
        //now the effect should NOT be subscribed to unrelatedSignal
        unrelatedSignal.set(10);
        assertThat(effectCalledCounter.getUntracked()).isEqualTo(1);
    }
}
