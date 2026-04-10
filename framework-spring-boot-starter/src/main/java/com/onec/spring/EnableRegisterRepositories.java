package com.onec.spring;

import org.springframework.data.repository.config.DefaultRepositoryBaseClass;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface EnableRegisterRepositories {

    String[] value() default {};

    String[] basePackages() default {};

    Class<?>[] basePackageClasses() default {};

    Class<?> repositoryBaseClass() default DefaultRepositoryBaseClass.class;
}
