package com.chock.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * @Author:kchwang
 * @Date:2024-12-14
 */
@Documented
@Target(ElementType.FIELD)//作用在域上，用于描述域
public @interface AutoWired {
    String value() default "";
}
