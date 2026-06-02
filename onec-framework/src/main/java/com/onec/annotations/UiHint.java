package com.onec.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Per-field UI hint.
 *
 * @deprecated Configure field hints via {@code UiLayoutBuilder} in your
 * {@code OneCUiConfigurer} instead, e.g.
 * {@snippet :
 *   layout.section("Sales").document(Invoice.class, d -> d
 *           .field("total").order(10).hideInForm()
 *           .field("notes").widget("textarea"));
 * }
 * Layout-configured hints override this annotation. The annotation will be
 * removed in the next release.
 *
 * <p><b>One exception:</b> tabular section row classes (e.g. line-item rows
 * inside a document's {@code @TabularSection}). The DSL does not yet expose
 * tabular section field hints; keep {@code @UiHint} on those row classes
 * until that capability lands.</p>
 */
@Deprecated(since = "next", forRemoval = true)
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
