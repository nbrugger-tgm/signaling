import eu.nitonfx.signaling.api.Context;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Consumer;

import static org.mockito.Mockito.*;

public class ListSignalTest {
    @Test
    void onAddEffectsRunCleanup() {
        var cx = Context.create();
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
