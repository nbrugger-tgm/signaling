package eu.nitonfx.signaling.processors.elementbuilder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PACKAGE)
@Retention(RetentionPolicy.SOURCE)
public @interface ReactiveBuilder {
    String NO_AGGREGATOR = "#";
    /**
     * @return a list of classes to create reactive builders for
     */
    Class<?>[] value();

    /**
     * @return the name of the class to put the builder methods in, if not set, no aggregator is created
     */
    String aggregatorClassname() default NO_AGGREGATOR;

    /**
     * Classes listed in this list should contain <b>static</b> methods that have at least two parameters:
     * <ul>
     *     <li>{@link eu.nitonfx.signaling.api.Context}</li>
     *     <li>The receiver type/the type to be extended, has to be one of {@link #value()}</li>
     * </ul>
     *
     * @return a set of classes that are added to the generated classes if suitable.
     */
    Class<?>[] extensions();
}
