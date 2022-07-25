package com.edu.netty.rpc.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ServiceFactory {

    static Properties properties;
    static Map<Class<?>, Object> map = new ConcurrentHashMap<>();

    static {
        try(InputStream in = ServiceFactory.class.getResourceAsStream("/application.properties")){
            properties = new Properties();
            properties.load(in);
            Set<String> names = properties.stringPropertyNames();
            names.forEach(name -> {
                if (name.endsWith("Service")) {
                    try {
                        Class<?> interfaceClass = Class.forName(name);
                        Class<?> instanceClass = Class.forName(properties.getProperty(name));
                        map.put(interfaceClass, instanceClass.newInstance());
                    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
            });

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T getService(Class<T> interfaceClass) {
        return (T) map.get(interfaceClass);
    }

}
