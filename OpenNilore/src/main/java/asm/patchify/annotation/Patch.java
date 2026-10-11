package asm.patchify.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Patch {
    Class<?> value() default void.class;

    /**
     * Alternative to {@link #value()} for targeting classes that are not available at compile time.
     * When non-empty, takes precedence over {@code value()}. This is useful for targeting classes from
     * optional mods (e.g. Embeddium/Sodium).
     * <p>Use the fully qualified JVM class name (e.g. {@code "me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockOcclusionCache"}).</p>
     */
    String className() default "";
}
