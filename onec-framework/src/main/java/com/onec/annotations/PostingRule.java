package com.onec.annotations;

import com.onec.model.AccumulationRecord;
import com.onec.model.MovementType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(PostingRules.class)
public @interface PostingRule {

    Class<? extends AccumulationRecord> register();

    MovementType movement();

    String forEach() default "";

    String[] map();
}
