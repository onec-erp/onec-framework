package com.onec.annotations;

import com.onec.model.Periodicity;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface InformationRegister {

    String name();

    Periodicity periodicity() default Periodicity.NONE;
}
