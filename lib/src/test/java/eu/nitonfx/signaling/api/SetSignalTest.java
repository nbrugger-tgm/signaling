package eu.nitonfx.signaling.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

abstract class SetSignalTest {
    abstract SetSignal<String> init(Context cx);
    void add(SetSignal<String> set,String s) {
        set.add(s);
    }
    void remove(SetSignal<String> set,String s) {
        set.remove(s);
    }
    protected Context cx;

    @BeforeEach
    void setContext(){
        cx = Context.create();
    }

    @Nested
    class Size {
        @Test void isTracked(){
            var set = init(cx);
            add(set, "a");
            add(set, "b");
            add(set, "c");
            Consumer<Integer> effect = mock();
            cx.createEffect(() -> {
                effect.accept(set.size());
            });
            verify(effect, times(1)).accept(3);
            add(set, "d");
            verify(effect, times(1)).accept(4);
            remove(set, "a");
            verify(effect, times(2)).accept(3);
            remove(set, "b");
            verify(effect, times(1)).accept(2);
            remove(set, "c");
            verify(effect, times(1)).accept(1);
        }
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
            assertThat(set.getUntracked()).containsExactlyInAnyOrderElementsOf(set);
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

            assertThat(iterElems).containsExactlyInAnyOrderElementsOf(set);
        }
    }
    @Nested
    class Iterator {
        @Test void shouldTrackHasNextInvocation(){
            var set = init(cx);
            add(set,"a");
            Runnable effect = mock();
            cx.createEffect(()->{
                set.iterator().hasNext();
                effect.run();
            });
            verify(effect,times(1)).run();
            reset(effect);
            add(set,"b");
            verify(effect,times(1)).run();
        }
        @Test void shouldTrackNextInvocation(){
            var set = init(cx);
            add(set,"a");
            Runnable effect = mock();
            cx.createEffect(()->{
                set.iterator().next();
                effect.run();
            });
            verify(effect,times(1)).run();
            reset(effect);
            add(set,"b");
            verify(effect,times(1)).run();
        }
        @Test void shouldNotAllowRemoval(){
            var set = init(cx);
            add(set,"a");
            var iterator = set.iterator();
            iterator.hasNext();
            iterator.next();
            assertThatExceptionOfType(UnsupportedOperationException.class)
                    .isThrownBy(iterator::remove);
        }
        @Test void shouldContainSameElements(){
            var set = init(cx);
            add(set,"a");
            add(set,"b");
            add(set,"c");

            var iterItems = new HashSet<String>();
            set.iterator().forEachRemaining(iterItems::add);

            assertThat(iterItems).containsExactlyInAnyOrderElementsOf(set);
        }
    }
    @Nested
    class OnAdd {
        @Test void shouldCallEffectOnNewElementInsertion(){
            var set = init(cx);
            var runnable = mock(Runnable.class);
            cx.run(()->{
               set.onAdd((e)->{
                   runnable.run();
               });
            });
            add(set,"b");
            verify(runnable,times(1)).run();
        }
        @Test void shouldCallEffectForExistingElements(){
            var set = init(cx);
            add(set,"a");
            add(set,"b");
            var runnable = mock(Runnable.class);
            cx.run(()->{
                set.onAdd((e)-> runnable.run());
            });
            verify(runnable,times(2)).run();

        }
        @Test void shouldCleanAddedItemEffectOnRemoval(){
            var set = init(cx);
            var runnable = mock(Runnable.class);
            cx.run(()->{
                set.onAdd((e)-> cx.cleanup(runnable::run));
            });
            add(set,"a");
            remove(set,"a");
            verify(runnable,times(1)).run();
        }
        @Test void shouldCleanAddedItemEffectOnManualCancel(){
            var set = init(cx);
            var runnable = mock(Runnable.class);
            final EffectHandle[] onAddEffect = new EffectHandle[1];
            cx.run(()-> {
                onAddEffect[0] = set.onAdd((e) -> cx.cleanup(runnable::run));
            });
            add(set,"a");
            onAddEffect[0].cancel();
            verify(runnable, times(1)).run();
        }
        @Test void shouldCleanInitialItemEffectOnRemoval(){
            var set = init(cx);
            add(set,"a");
            var runnable = mock(Runnable.class);
            cx.run(()-> {
                set.onAdd((e) -> cx.cleanup(runnable::run));
            });
            remove(set,"a");
            verify(runnable, times(1)).run();
        }
        @Test void shouldCleanInitialItemEffectOnManualCancel(){
            var set = init(cx);
            add(set,"a");
            EffectHandle[] onAddEffect = new EffectHandle[1];
            var runnable = mock(Runnable.class);
            cx.run(()-> {
                onAddEffect[0] = set.onAdd((e) -> cx.cleanup(runnable));
            });
            onAddEffect[0].cancel();
            verify(runnable, times(1)).run();
        }
        @Test void shouldNotRerunEffectsOfNonModifiedItems(){
            var set = init(cx);
            add(set,"a");
            add(set,"b");
            add(set,"c");
            Consumer<String> cleanup = mock();
            Consumer<String> effect = mock();
            cx.run(()-> {
                set.onAdd((e) -> {
                    effect.accept(e);
                    cx.cleanup(() -> cleanup.accept(e));
                });
            });
            remove(set,"b");
            verify(cleanup, times(0)).accept("a");
            verify(cleanup, times(0)).accept("c");
            verify(effect, times(1)).accept("a");
            verify(effect, times(1)).accept("c");
        }
        @Test void shouldCallEffectWithCorrectElement(){
            var set = init(cx);
            add(set,"a");
            Consumer<String> effect = mock();
            cx.run(()-> {
                set.onAdd(effect);
            });
            verify(effect, times(1)).accept("a");
            add(set,"b");
            verify(effect, times(1)).accept("b");
        }
    }
}
abstract class MappableSignalTest extends SetSignalTest {
    abstract class MapTest extends SetSignalTest {

        private SetSignal<String> parent;

        abstract SetSignal<String> initParent(Context cx);

        @Override
        void add(SetSignal<String> set, String s) {
            MappableSignalTest.this.add(parent, s);
        }

        @Override
        void remove(SetSignal<String> set, String s) {
            MappableSignalTest.this.remove(parent,s);
        }

        @Override
        SetSignal<String> init(Context cx) {
            parent = initParent(cx);
            return parent.map(Function.identity());
        }

        @Test void shouldApplyMapping(){
            var base = MappableSignalTest.this.init(cx);
            var mapped = base.map(str -> str+"-mapped");
            base.add("a");
            base.add("b");
            base.add("c");
            assertThat(mapped)
                    .containsExactlyInAnyOrder("a-mapped", "b-mapped", "c-mapped");
            assertThat(mapped.untrackedIterator())
                    .toIterable()
                    .containsExactlyInAnyOrder("a-mapped", "b-mapped", "c-mapped");
            assertThat(mapped.iterator())
                    .toIterable()
                    .containsExactlyInAnyOrder("a-mapped", "b-mapped", "c-mapped");
            Consumer<String> consumer = mock();
            cx.run(()->mapped.onAdd(consumer));
            verify(consumer).accept("a-mapped");
            verify(consumer).accept("b-mapped");
            verify(consumer).accept("c-mapped");
            assertThat(mapped.contains("b-mapped")).isTrue();
            assertThat(mapped.getUntracked())
                    .containsExactlyInAnyOrder("a-mapped","b-mapped","c-mapped");
        }
    }
    @Nested
    class Map extends MapTest {

        @Override
        SetSignal<String> initParent(Context cx) {
            return MappableSignalTest.this.init(cx);
        }

        @DisplayName("map()")
        @Nested class MapMap extends MapTest {
            @Override
            SetSignal<String> initParent(Context cx) {
                return Map.this.init(cx);
            }

            @Override
            void add(SetSignal<String> set, String s) {
                Map.this.add(set, s);
            }

            @Override
            void remove(SetSignal<String> set, String s) {
                Map.this.remove(set, s);
            }
        }
    }
}
@DisplayName("Context.createSetSignal()")
@DisplayNameGeneration(DisplayNameGenerator.IndicativeSentences.class)
class CreateSignal extends MappableSignalTest {
    @Override
    SetSignal<String> init(Context cx) {
        return cx.createSetSignal();
    }
}