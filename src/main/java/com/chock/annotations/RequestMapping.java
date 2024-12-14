package com.chock.annotations;

import java.lang.annotation.*;

/**
 * @Author:kchwang
 * @Date:2024-12-14
 */

@Documented
@Target({ElementType.TYPE,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestMapping {
    String value() default "";
}
