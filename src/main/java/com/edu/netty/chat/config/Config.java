package com.edu.netty.chat.config;

import com.edu.netty.protocal.Serializer;

import java.io.InputStream;
import java.util.Properties;

public class Config {

    static Properties properties;

    static {
        try(InputStream in = Config.class.getResourceAsStream("/application.properties")) {
            properties = new Properties();
            properties.load(in);
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取用户配置的服务端口
     * -----------------------
     * 默认配置为 ： 8000
     * @return
     */
    public static int getServerPort(){
        String value = properties.getProperty("server.port");
        if (value == null) {
            return 8000;
        }else {
            return Integer.parseInt(value);
        }
    }

    /**
     * 获取用户自定义的序列化算法
     * -----------------------
     * 默认配置为 ： Java 自带的序列化算法
     * @return
     */
    public static Serializer.Algorithm getSerializerAlgorithm() {
        String value = properties.getProperty("serializer.algorithm");
        if (value == null) {
            return Serializer.Algorithm.Java;
        } else {
            return Serializer.Algorithm.valueOf(value);
        }
    }
}
