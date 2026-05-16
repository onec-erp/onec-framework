package com.onec.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(DashboardWidgets.class)
public @interface DashboardWidget {
    String title();
    String type();
    int order() default 0;
    String width() default "1/3";
    int maxItems() default 10;
    String dateField() default "";
    String titleField() default "";
    String[] extraConfig() default {};
}
