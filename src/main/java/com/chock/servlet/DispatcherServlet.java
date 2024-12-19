package com.chock.servlet;

import com.chock.annotations.AutoWired;
import com.chock.annotations.Controller;
import com.chock.annotations.RequestMapping;
import com.chock.annotations.Service;
import com.chock.pojo.Handler;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @Description:前段控制器
 *
 * @Author:kchwang
 * @Date:2024-12-17
 */

public class DispatcherServlet extends HttpServlet{
     private Properties properties= new Properties();
     //缓存扫描类的全类名（成为私有的只能在该类中使用）
     private List<String> classNames = new ArrayList<String>();
     //ioc容器
    private Map<String,Object> ioc = new HashMap<String,Object>();
    //handlerMapping
    private List<Handler> handlerMapping = new ArrayList<>();
    //servlet的生命周期的初始化
    @Override
    public void init(ServletConfig config) throws ServletException{
        //1.加载配置文件springmvc.properties，这个搭配xml文件可以将xml中的内容获取出来<param-name>
        config.getInitParameter("contextConfigLocation");
        //扫描相关的类，扫描注解
        //properties是Properties类型的对象，通常用来存储配置文件中的键值对数据。在java中，Properties是HashTable的子类。
        // getProperty(String key)：该方法用于从 properties 对象中根据键（key）获取对应的值
        doScan(properties.getProperty("contextConfigLocation"));

        //初始化bean对象（实现ioc容器，基于注解），使用注解的方式
        doInstance();
        //实现依赖注入
        doAutowired();
        //构造一个HandlerMapping处理器映射器，将配制好的url和method建立映射关系
        initHandlerMapping();
    }

    /**
     *  构造一个HandlerMapping处理器映射器(最关键的环节)
     */
    private void initHandlerMapping(){
        if(ioc.isEmpty()){
            return;
        }

        for(Map.Entry<String,Object> entry:ioc.entrySet()){
            //获取当前遍历的对象的class类型
            Class<?> aClass = entry.getValue().getClass();
            //不是被Autowired修饰的类，则直接continue跳过
            if(!aClass.isAnnotationPresent(AutoWired.class)){
                continue;
            }
            //定义url
            String baseUrl = "";
            if (aClass.isAnnotationPresent(RequestMapping.class)) {
                //如果是请求类型的注解修饰的类
                RequestMapping annotation = aClass.getAnnotation(RequestMapping.class);
                baseUrl = annotation.value();
            }
            //获取方法
            Method[] methods = aClass.getMethods();
            for (int i = 0; i < methods.length; i++) {
                Method method = methods[i];

                //方法没有标识RequestMapping的就不处理
                if(!method.isAnnotationPresent(RequestMapping.class)){
                    continue;
                }

                //如果标识的话就处理
                RequestMapping annotation = method.getAnnotation(RequestMapping.class);
                String methodUrl = annotation.value();
                String url = baseUrl+=methodUrl;
                //把method所有信息及url封装成一个Handler
                Handler handler = new Handler(entry.getValue(), method, Pattern.compile(url));

                //计算方法的参数位置信息
                Parameter[] parameters = method.getParameters();
                for (int j = 0; j < parameters.length; j++) {
                    Parameter parameter = parameters[j];
                    //如果是request或者response对象，那么参数名称写的HttpServletRequest和HttpServletResponse
                    if(parameter.getType()== HttpServletRequest.class||parameter.getType()== HttpServletResponse.class){
                        handler.getParamIndexMapping().put(parameter.getType().getSimpleName(), j);
                    }else{
                        handler.getParamIndexMapping().put(parameter.getName(), j);
                    }
                }
                handlerMapping.add(handler);
            }

        }


    }


    /**
     * 实现依赖注入
     */
    public void doAutowired(){
        //如果对象为空则直接返回
        if(ioc.isEmpty()){
            return;
        }
        //有对象的话进行依赖注入
        //便利ioc中的所有对象，查看对象中的字段，是否包含@Autowired注解，如果有的话需要维护依赖注入关系
        for(Map.Entry<String,Object> entry:ioc.entrySet()){
            Field[] declaredFields = entry.getValue().getClass().getDeclaredFields();
            for(int i=0; i<declaredFields.length; i++){
                Field declaredField = declaredFields[i];
                //如果不是@Autowired类型的则跳过
                if (!declaredField.isAnnotationPresent(Service.class)){
                    continue;
                }

                //有该注解
                AutoWired annotation = declaredField.getAnnotation(AutoWired.class);
                //
                String beanName = annotation.value();
                if("".equals(beanName.trim())){
                    //没有具体的bean，id，那就需要根据当前字段类型注入（接口注入）
                    beanName = declaredField.getName();
                }
                //开启赋值
                declaredField.setAccessible(true);
                try {
                    declaredField.set(entry.getValue(),ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }


    /**
     * ioc容器，基于classnames缓存的类的全限定类名，以及反射技术，完成对象创建和管理
     */
    public void doInstance(){
        if(classNames.size()==0){
            return;
        }

        try {
            for (int i = 0; i < classNames.size(); i++) {
                //这里使用到了反射
                String className = classNames.get(i);
                //反射（动态加载类，反射）
                //动态加载：允许在运行时根据类名加载
                Class<?> aClass = Class.forName(className);
                //区分controller，区分service
                //isisAnnotationPresent:来修饰某个类是否被某个注解所标记
                if(aClass.isAnnotationPresent(Controller.class)){
                    //controller的id不做过多处理，不取value，就拿类的首字母小写作为id，保存到ioc中
                    String simpleName = aClass.getSimpleName();
                    String lowerFirstSimpleName = LowerFirst(simpleName);
                    // 获取无参构造方法
                    Constructor<?> constructor = aClass.getConstructor();
                    Object o = constructor.newInstance();
                    ioc.put(lowerFirstSimpleName, o);
                } else if (aClass.isAnnotationPresent(Service.class)) {
                    Service annotation = aClass.getAnnotation(Service.class);
                    //获得注解的名字
                    String beanName = annotation.value();
                    //如果指定了id，就以指定的为准
                    if (""!=beanName.trim()){
                        ioc.put(beanName, ioc.get(beanName));
                    }else {
                        //如果没指定就以首字母小写为准
                        beanName  = LowerFirst(aClass.getSimpleName());
                        ioc.put(beanName, aClass.newInstance());
                    }

                    //service层往往是有接口的，面向接口开发，这时以接口名为id，放入一份对象放入到ioc中，以便后期根据接口类型注入
                    Class<?>[] anInterfaces = aClass.getInterfaces();
                    for (int j = 0; j < anInterfaces.length; j++) {
                        Class<?> anInterface =anInterfaces[j];
                        //以接口的全类名作为id放入ioc中
                        ioc.put(anInterface.getName(), aClass.newInstance());
                    }
                }else{
                    continue;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 小写首字母
     */
    public String LowerFirst(String str){
        char[] chars= str.toCharArray();
        if(chars[0]>='A' && chars[0]<='Z'){
            chars[0]= (char) (chars[0]+32);
        }
        return String.valueOf(chars);
    }

    /**
     *扫描相关的类：磁盘上的文件，这个写法值得自己深入研究
     */
    private void doScan(String scanPackage){
        //使用线程获取类加载器的上下文类加载器
        //getResource（）：从类路径当中获取资源，返回的是一个url对象，指向类路径当中的指定资源
        //getResource（""）:获取当前类路径的根目录，如果为空的话，通常会返回类路径的根目录
        //getPath：从URL当中获取文件的路径部分

        String scanPackagePath=Thread.currentThread().getContextClassLoader().getResource("").getPath()+scanPackage;

        //创建文件包
        File file = new File(scanPackagePath);
        //将文件包转换为一个数组
        File[] files = file.listFiles();
        //自己盲目了（在这些获得的数组或者集合，对象的时候一定要进行判空）
        if(files!=null && files.length>0){
            for (int i = 0; i < files.length; i++) {
                if(files[i].isDirectory()){
                    //递归调用此循环继续查找对应的目录文件,如果是目录的话就继续执行
                    doScan(scanPackage+"."+files[i].getName());
                } else if (files[i].getName().endsWith(".class")) {
                    //已经找到该类了
                    String className = scanPackage+"."+files[i].getName().replaceAll(".class", "");
                    classNames.add(className);
                }
            }
        }

    }

    /**
     * 加载配置文件(这个该怎么实现呢，用到了文件流)
     */
    private void doLoadConfig(String contextConfigLocation){
        //加载contextConfigLocation的配置文件，将其转换为stream。
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);

        //加载配置文件(抛异常，要么用throws抛出异常)。
        try {
            properties.load(resourceAsStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}




