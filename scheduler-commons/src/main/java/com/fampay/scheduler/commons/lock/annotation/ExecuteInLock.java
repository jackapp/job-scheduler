package com.fampay.scheduler.commons.lock.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ExecuteInLock {
    String prefix() default "";
    @Deprecated
    String lockIdKey() default "";
    String[] lockIdKeys() default {};
    long waitingTime() default 0;
    boolean suppressError() default false;
}
