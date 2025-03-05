import eu.nitonfx.signaling.api.Context;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.mockito.Mockito.*;

public class UseMemoTest {
    @Test
    void memoNotEagerlyCalculated(){
        Supplier<String> producer = mock();
        var cx = Context.create();
        cx.run(()-> cx.createMemo(producer));
        verify(producer,never()).get();
    }

    @Test
    void memoCalculatedOnGet(){
        Supplier<String> producer = mock();
        var cx = Context.create();
        cx.run(()-> {
            cx.createMemo(producer);
            producer.get();
        });
        verify(producer,times(1)).get();
    }

    @Test
    void memoCalculatedOnEffectFetch(){
        Supplier<String> producer = mock();
        var cx = Context.create();
        cx.run(()-> cx.createEffect(producer::get));
        verify(producer,times(1)).get();
    }

    @Test
    void memoOnlyCalculatedOnce(){
        Supplier<String> producer = mock();
        var cx = Context.create();
        cx.run(()-> {
            var memo = cx.createMemo(()->producer.get());
            memo.get();
            memo.get();
            memo.get();
            memo.get();
        });
        verify(producer,times(1)).get();
    }

    @Test
    void memoNotRecalculatedOnValueChangeBeforeGetting(){
        var onCalculate = mock(Runnable.class);
        var cx = Context.create();
        cx.run(()->{
            var signal = cx.createSignal(0);
            var memo = cx.createMemo(()-> {
                onCalculate.run();
                return signal.get() * 2;
            });
            signal.set(2);
            signal.set(3);
            signal.set(4);
            memo.get();
        });
        verify(onCalculate,times(1)).run();
    }
    @Test
    void memoNotRecalculatedOnValueChangeAfterGetting(){
        var onCalculate = mock(Runnable.class);
        var cx = Context.create();
        cx.run(()->{
            var signal = cx.createSignal(0);
            var memo = cx.createMemo(()-> {
                onCalculate.run();
                return signal.get() * 2;
            });
            memo.get();
            signal.set(2);
            signal.set(3);
            signal.set(4);
        });
        verify(onCalculate,times(1)).run();
    }
    @Test
    void memoRecalculatedOnValueChangeWhenRequested(){
        var onCalculate = mock(Runnable.class);
        var cx = Context.create();
        cx.run(()->{
            var signal = cx.createSignal(0);
            var memo = cx.createMemo(()-> {
                onCalculate.run();
                return signal.get() * 2;
            });
            memo.get();
            signal.set(2);
            memo.get();
        });
        verify(onCalculate,times(2)).run();
    }

    @Test
    void nonChangingUpdatesDoNotPush(){
        Consumer<String> consumer = mock();
        var cx = Context.create();
        var signal = cx.createSignal(0);
        var isOdd = cx.createMemo(()->signal.get() %2 == 1);
        var oddString = cx.createMemo(()->isOdd.get() ? "odd" : "even");
        cx.run(()-> cx.createEffect(()-> consumer.accept(oddString.get())));
        verify(consumer).accept("even");
        signal.set(2);
        verifyNoMoreInteractions(consumer);
    }

    @Test
    void nonChangingUpdatesDoNotRecalculate(){
        Runnable stringifyCalc = mock();
        var cx = Context.create();
        var signal = cx.createSignal(1);
        var isOdd = cx.createMemo(()->signal.get() %2 == 1);
        var oddString = cx.createMemo(()-> {
            stringifyCalc.run();
            return isOdd.get() ? "odd" : "even";
        });
        cx.run(()-> cx.createEffect(oddString::get));
        signal.set(11);
        signal.set(13);
        signal.set(3);
        verify(stringifyCalc, times(1)).run();
    }


    @Test
    void changingUpdatesPushUpdate(){
        Consumer<String> consumer = mock();
        var cx = Context.create();
        var signal = cx.createSignal(0);
        var isOdd = cx.createMemo(()->signal.get() %2 == 1);
        var oddString = cx.createMemo(()->isOdd.get() ? "odd" : "even");
        cx.run(()-> cx.createEffect(()-> consumer.accept(oddString.get())));
        verify(consumer).accept("even");
        signal.set(3);
        verify(consumer).accept("odd");
    }
}
