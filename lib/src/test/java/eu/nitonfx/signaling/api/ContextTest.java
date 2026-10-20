package eu.nitonfx.signaling.api;

import eu.nitonfx.signaling.SentenceDisplayNameGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.Nested;

@DisplayNameGeneration(SentenceDisplayNameGenerator.class)
public abstract class ContextTest {

    protected abstract Context init();

    @Nested
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
    class CreateEffect extends UseEffectTest {
        @Override
        protected Context createContext() {
            return ContextTest.this.init();
        }
    }

    @Nested
    class CreateMemo extends UseMemoTest {

        @Override
        Context createContext() {
            return ContextTest.this.init();
        }
    }

    @Nested
    class CreateListSignal extends ListSignalTest {

        @Override
        Context createContext() {
            return ContextTest.this.init();
        }
    }
}
@DisplayName("Context")
@DisplayNameGeneration(SentenceDisplayNameGenerator.class)
class ContextStaticTest {

    @Nested
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
