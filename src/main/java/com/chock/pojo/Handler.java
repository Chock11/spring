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

    public Handler(Object controller, Method method, Pattern pattern) {
        this.controller = controller;
        this.method = method;
        this.pattern = pattern;
    }

    public Object getController() {
        return controller;
    }
    public void setController(Object controller) {
        this.controller = controller;
    }
    public Method getMethod() {
        return method;
    }
    public void setMethod(Method method) {
        this.method = method;
    }
    public Pattern getPattern() {
        return pattern;
    }
    public void setPattern(Pattern pattern) {
        this.pattern = pattern;
    }
    public Map<String, Integer> getParamIndexMapping() {
        return paramIndexMapping;
    }
    public void setParamIndexMapping(Map<String, Integer> paramIndexMapping) {
        this.paramIndexMapping = paramIndexMapping;
    }
}
