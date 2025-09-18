package eu.nitonfx.signaling.api;

import org.junit.jupiter.api.Test;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.mockito.Mockito.*;

public abstract class UseMemoTest {
    abstract Context createContext();
    @Test
    void memoNotEagerlyCalculated(){
        Supplier<String> producer = mock();
        var cx = createContext();
        cx.run(()-> cx.createMemo(producer));
        verify(producer,never()).get();
    }

    @Test
    void memoCalculatedOnGet(){
        Supplier<String> producer = mock();
        var cx = createContext();
        cx.run(()-> {
            cx.createMemo(producer);
            producer.get();
        });
        verify(producer,times(1)).get();
    }

    @Test
    void memoCalculatedOnEffectFetch(){
        Supplier<String> producer = mock();
        var cx = createContext();
        cx.run(()-> cx.createEffect(producer::get));
        verify(producer,times(1)).get();
    }

    @Test
    void memoOnlyCalculatedOnce(){
        Supplier<String> producer = mock();
        var cx = createContext();
        cx.run(()-> {
            var memo = cx.createMemo(producer::get);
            cx.createEffect(()-> {
                memo.get();
                memo.get();
            });
        });
        verify(producer,times(1)).get();
    }

    @Test
    void memoNotRecalculatedOnValueChangeBeforeGetting(){
        var onCalculate = mock(Runnable.class);
        var cx = createContext();
        cx.run(()->{
            var signal = cx.createSignal(0);
            var memo = cx.createMemo(()-> {
                onCalculate.run();
                return signal.get() * 2;
            });
            cx.createEffect(()-> {
                signal.set(2);
                signal.set(3);
                signal.set(4);
            });
            cx.createEffect(()-> {
                memo.get();
            });
        });
        verify(onCalculate,times(1)).run();
    }
    @Test
    void memoNotRecalculatedOnValueChangeAfterGetting(){
        var onCalculate = mock(Runnable.class);
        var cx = createContext();
        cx.run(()->{
            var signal = cx.createSignal(0);
            var memo = cx.createMemo(()-> {
                onCalculate.run();
                return signal.get() * 2;
            });
            memo.getUntracked();
            cx.createEffect(()->{
                signal.set(2);
                signal.set(3);
                signal.set(4);
            });
        });
        verify(onCalculate,times(1)).run();
    }
    @Test
    void memoRecalculatedOnValueChangeWhenRequested(){
        var onCalculate = mock(Runnable.class);
        var cx = createContext();
        cx.run(()->{
            var signal = cx.createSignal(0);
            var memo = cx.createMemo(()-> {
                onCalculate.run();
                return signal.get() * 2;
            });
            cx.createEffect(()-> memo.get());
            cx.createEffect(()->{
                signal.set(2);
                signal.set(4);
            });
        });
        //once calculated from the initial value (0) and then with the end-result of the change (4)
        verify(onCalculate,times(2)).run();
    }

    @Test
    void nonChangingUpdatesDoNotPush(){
        Consumer<String> consumer = mock();
        var cx = createContext();
        var signal = cx.createSignal(0);
        var isOdd = cx.createMemo(()->signal.get() %2 == 1);
        var oddString = cx.createMemo(()->isOdd.get() ? "odd" : "even");
        cx.run(()-> cx.createEffect(()-> consumer.accept(oddString.get())));
        verify(consumer).accept("even");
        signal.set(2);
        //since oddString doesn't change here the subscribing effect should not be re run
        verifyNoMoreInteractions(consumer);
    }

    @Test
    void nonChangingUpdatesDoNotRecalculate(){
        Runnable stringifyCalc = mock();
        var cx = createContext();
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
        //since isOdd never changed to false there was no need to re-run the oddString calc
        verify(stringifyCalc, times(1)).run();
    }


    @Test
    void changingUpdatesPushUpdate(){
        Consumer<String> consumer = mock();
        var cx = createContext();
        var signal = cx.createSignal(0);
        var isOdd = cx.createMemo(()->signal.get() %2 == 1);
        var oddString = cx.createMemo(()->isOdd.get() ? "odd" : "even");
        cx.run(()-> cx.createEffect(()-> consumer.accept(oddString.get())));
        verify(consumer).accept("even");
        signal.set(3);
        verify(consumer).accept("odd");
    }
}
