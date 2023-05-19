package eu.nitonfx.signaling;

import java.util.function.Function;

public interface Subscribable extends Function<Runnable, Subscription> {
    Subscription subscribe(Runnable listener);

    @Override
    default Subscription apply(Runnable runnable){
        return subscribe(runnable);
    }
}
