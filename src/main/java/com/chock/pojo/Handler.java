package com.chock.pojo;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @Author:kchwang
 * @Date:2024-12-14
 */

public class Handler {
    private Object controller;

    //支持正则表达式
    private Method method;

    private Pattern pattern;

    //顺序绑定，key是参数，alue代表参数
    private Map<String, Integer> paramIndexMapping;

    public Handler(Object controller, Method method, Pattern pattern) {}
}
