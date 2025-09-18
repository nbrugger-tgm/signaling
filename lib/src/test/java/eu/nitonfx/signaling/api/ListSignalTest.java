package eu.nitonfx.signaling.api;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Consumer;

import static org.mockito.Mockito.*;

public abstract class ListSignalTest {
    abstract Context createContext();
    @Test
    void onAddEffectsRunCleanup() {
        var cx = createContext();
        Consumer<String> cleanup = mock();
        var outerEffect = cx.createEffect(() -> {
            var list = cx.createSignal(List.of("A", "B"));
            list.onAdd((str, index) -> {
                cx.cleanup(() -> cleanup.accept(str.get()));
            });
        });
        verifyNoInteractions(cleanup);
        outerEffect.cancel();
        verify(cleanup).accept("A");
        verify(cleanup).accept("B");
    }
}
