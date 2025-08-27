package eu.nitonfx.signaling.api;

import java.util.function.Supplier;

/**
 * Represents a handle for an effect that allows manual cancellation.
 * The cancel method can be used to stop the effect and clean up any resources associated with it.
 */
public interface EffectHandle {
    static EffectHandle of(String type, Runnable cancel, Supplier<String> childrenTree) {
        return new EffectHandle() {
            private String name;

            @Override
            public String toString() {
                return "%s(%s)".formatted(type, name);
            }

            @Override
            public void cancel() {
                cancel.run();
            }

            @Override
            public void name(String name) {
                this.name = name;
            }

            @Override
            public String formatAsTree() {
                return this +"\n"+(childrenTree.get().replace("\n","\n  "));
            }
        };
    }

    void cancel();

    /**
     * Only useful for debugging purposes. Gives the effect a name
     */
    void name(String name);

    String formatAsTree();
}
