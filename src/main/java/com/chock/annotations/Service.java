package com.chock.annotations;

import java.lang.annotation.*;

/**
 *
 * @Author:kchwang
 * @Date:2024-12-14
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Service {
    String value() default "";
}
