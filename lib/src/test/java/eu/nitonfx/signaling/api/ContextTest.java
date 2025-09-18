package eu.nitonfx.signaling.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

@DisplayName("Context")
public abstract class ContextTest {

    protected abstract Context init();

    @Nested
    @DisplayName("createSetSignal()")
    class CreateSetSignal extends SetSignalTest {

        @Override
        SetSignal<String> init(Context cx) {
            return cx.createSetSignal();
        }

        @Override
        Context createContext() {
            return ContextTest.this.init();
        }
    }

    @Nested
    @DisplayName("createEffect()")
    class CreateEffect extends UseEffectTest {
        @Override
        protected Context createContext() {
            return ContextTest.this.init();
        }
    }

    @Nested
    @DisplayName("createMemo()")
    class CreateMemo extends UseMemoTest {

        @Override
        Context createContext() {
            return ContextTest.this.init();
        }
    }

    @Nested
    @DisplayName("createListSignal()")
    class CreateListSignal extends ListSignalTest {

        @Override
        Context createContext() {
            return ContextTest.this.init();
        }
    }
}
@DisplayName("Context")
class ContextStaticTest {

    @Nested
    @DisplayName("create()")
    class Create extends ContextTest {

        @Override
        protected Context init() {
            return Context.create();
        }
    }

    @Nested
    @DisplayName("global")
    class Global extends ContextTest {

        @Override
        protected Context init() {
            return Context.global;
        }
    }
}
