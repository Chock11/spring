package com.chock.annotations;

import java.lang.annotation.*;

/**
 *注解开发
 *
 * @Author：kchwang
 * @Date:2024-12-14
 */
@Documented //表明注解要被javadoc工具记录
@Target(ElementType.TYPE) //设定注解范围，用于描述类和接口上或enum声明
@Retention(RetentionPolicy.RUNTIME) //运行时有效
public @interface Controller {
    String value() default "";
}
