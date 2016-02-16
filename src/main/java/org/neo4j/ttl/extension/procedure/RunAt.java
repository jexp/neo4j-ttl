package org.neo4j.ttl.extension.procedure;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * @author mh
 * @since 12.02.16
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RunAt {
    enum Stage {Start, Runtime, Stop}

    Stage value() default Stage.Runtime;

    int delay() default 60;

    int repeat() default 60;

    TimeUnit unit() default TimeUnit.SECONDS;

}
