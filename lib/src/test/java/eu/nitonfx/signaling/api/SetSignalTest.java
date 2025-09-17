package eu.nitonfx.signaling.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

abstract class SetSignalTest {
    abstract SetSignal<String> init(Context cx);
    void add(SetSignal<String> set,String s) {
        set.add(s);
    }
    private Context cx;

    @BeforeEach
    void setContext(){
        cx = Context.create();
    }

    @Nested
    class GetUntracked {
        @Test void shouldNotReturnNull(){
            assertThat(init(cx).getUntracked()).isNotNull();
        }
        @Test void shouldContainSameElements(){
            var set = init(cx);
            add(set,"a");
            add(set,"b");
            add(set,"c");
            assertThat(set.getUntracked()).containsExactlyElementsOf(set);
        }
        @Test void shouldNotTrackSizeReads(){
            var set = init(cx);
            Runnable effect = mock();
            cx.createEffect(()->{
                set.getUntracked().size();
                effect.run();
            });
            verify(effect,times(1)).run();
            add(set,"a");
            add(set,"b");
            verify(effect,times(1)).run();
        }
        @Test void shouldNotTrackIteratorReads(){
            var set = init(cx);
            add(set,"a");

            Runnable effect = mock();
            cx.createEffect(()->{
                var iter = set.getUntracked().iterator();
                if(iter.hasNext()) iter.next();
                effect.run();
            });
            verify(effect,times(1)).run();
            add(set,"b");
            verify(effect,times(1)).run();
        }
        @Test void shouldNotTrackContainReads(){
            var set = init(cx);
            add(set,"a");

            Runnable effect = mock();
            cx.createEffect(()->{
                set.getUntracked().contains("b");
                effect.run();
            });
            verify(effect,times(1)).run();
            add(set,"b");
            add(set,"c");
            verify(effect,times(1)).run();
        }
    }
    @Nested
    class UntrackedIterator {
        @Test void shouldNotTrackInvocation(){
            var set = init(cx);
            Runnable effect = mock();
            cx.createEffect(()->{
                set.untrackedIterator();
                effect.run();
            });
            add(set,"a");
            add(set,"b");
            verify(effect,times(1)).run();
        }
        @Test void shouldNotTrackHasNextInvocation(){
            var set = init(cx);
            Runnable effect = mock();
            cx.createEffect(()->{
                set.untrackedIterator().hasNext();
                effect.run();
            });
            add(set,"a");
            add(set,"b");
            verify(effect,times(1)).run();

        }
        @Test void shouldNotTrackNextInvocation(){
            var set = init(cx);
            add(set,"a");
            Runnable effect = mock();
            cx.createEffect(()->{
                set.untrackedIterator().next();
                effect.run();
            });
            add(set,"b");
            add(set,"c");
            verify(effect,times(1)).run();
        }
        @Test void shouldForwardRemoveToSet(){
            var set = init(cx);
            add(set,"a");
            add(set,"b");
            add(set,"c");
            var iter = set.untrackedIterator();
            var removed = iter.next();
            iter.remove();
            assertThat(set).doesNotContain(removed);
        }
        @Test void shouldTriggerUpdatesOnRemove(){
            var set = init(cx);
            add(set,"a");
            add(set,"b");
            add(set,"c");
            Consumer<Boolean> effect = mock();
            cx.createEffect(()->{
                effect.accept(set.contains("b"));
            });
            verify(effect,times(1)).accept(true);
            reset(effect);
            var iter = set.untrackedIterator();
            while(iter.hasNext() && !iter.next().equals("b")){}
            iter.remove();
            verify(effect,times(1)).accept(false);
        }
        @Test void shouldContainSameElements(){
            var set = init(cx);
            add(set,"a");
            add(set,"b");
            add(set,"c");

            var iterElems = new HashSet<String>();
            set.untrackedIterator().forEachRemaining(iterElems::add);

            assertThat(iterElems).containsExactlyElementsOf(set);
        }
    }
    @Nested
    class Iterator {
        void shouldTrackHasNextInvocation(){}
        void shouldTrackNextInvocation(){}
        void shouldNotAllowRemoval(){}
        void shouldContainSameElements(){}
    }
    @Nested
    class OnAdd {
        void shouldCallEffectOnNewElementInsertion(){}
        void shouldCallEffectForExistingElements(){}
        void shouldCleanAddedItemEffectOnRemoval(){}
        void shouldCleanAddedItemEffectOnManualCancel(){}
        void shouldCleanInitialItemEffectOnRemoval(){}
        void shouldCleanInitialItemEffectOnManualCancel(){}
        void shouldNotRerunEffectsOfNonModifiedItems(){}
        void shouldCallEffectWithCorrectElement(){}
    }
}

class CreateSignal extends SetSignalTest {
    @Override
    SetSignal<String> init(Context cx) {
        return cx.createSignal(Set.of());
    }

    @Nested
    class Map extends SetSignalTest {
        SetSignal<String> parent;

        @Override
        SetSignal<String> init(Context cx) {
            parent = CreateSignal.this.init(cx);
            return parent.map(Function.identity());
        }

        @Override
        void add(SetSignal<String> set, String s) {
            super.add(parent, s);
        }

        void shouldApplyMapping(){}
    }
}