package com.alibaba.fastjson.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author wenshao[szujobs@hotmail.com]
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
public @interface JSONField {
    /**
     * config encode/decode ordinal
     *
     * @return
     * @since 1.1.42
     */
    int ordinal() default 0;

    String name() default "";

    String format() default "";

    boolean serialize() default true;

    boolean deserialize() default true;


    String label() default "";

    /**
     * @since 1.2.12
     */
    boolean jsonDirect() default false;

    /**
     * Serializer class to use for serializing associated value.
     *
     * @since 1.2.16
     */
    Class<?> serializeUsing() default Void.class;

    /**
     * Deserializer class to use for deserializing associated value.
     *
     * @since 1.2.16
     */
    Class<?> deserializeUsing() default Void.class;

    /**
     * @return the alternative names of the field when it is deserialized
     * @since 1.2.21
     */
    String[] alternateNames() default {};

    /**
     * @since 1.2.31
     */
    boolean unwrapped() default false;

    /**
     * Only support Object
     *
     * @since 1.2.61
     */
    String defaultValue() default "";
}