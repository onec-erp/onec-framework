package com.onec.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface UiHint {
    boolean visibleInList() default true;
    boolean visibleInForm() default true;
    boolean visibleInDetail() default true;
    int order() default 0;
    String group() default "";
    String width() default "";

    /**
     * Controls the input widget rendered in the form.
     * Values: "" (default input), "textarea", "richtext"
     */
    String widget() default "";
}
