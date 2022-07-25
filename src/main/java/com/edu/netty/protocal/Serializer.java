package com.edu.netty.protocal;

import com.google.gson.*;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

public interface Serializer {

     <T> T deserializer(Class<T> clazz, byte[] bytes);

     <T> byte[] serializer(T obj);

    enum Algorithm implements Serializer {
        // JDK
        Java{
            @Override
            public <T> T deserializer(Class<T> clazz, byte[] bytes) {
                try {
                    ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
                    return (T) ois.readObject();
                } catch (ClassNotFoundException | IOException  e) {
                    throw new RuntimeException("发序列化失败", e);
                }
            }

            @Override
            public <T> byte[] serializer(T obj) {
                try {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(bos);
                    oos.writeObject(obj);
                    return bos.toByteArray();
                } catch (IOException e) {
                    throw new RuntimeException("序列化失败", e);
                }
            }
        },
        // JSON
        Json {
            @Override
            public <T> T deserializer(Class<T> clazz, byte[] bytes) {
                // 使用配置了转换器的Gson
                Gson gson = new GsonBuilder()
                        .registerTypeAdapter(Class.class, new ClassCodec()).create();
                String json = new String(bytes, StandardCharsets.UTF_8);
                return gson.fromJson(json, clazz);
            }

            @Override
            public <T> byte[] serializer(T obj) {
                Gson gson = new GsonBuilder()
                        .registerTypeAdapter(Class.class, new ClassCodec()).create();
                String json = gson.toJson(obj);
                return json.getBytes(StandardCharsets.UTF_8);
            }
        },
        // Protobuf
        Protobuf {
            @Override
            public <T> T deserializer(Class<T> clazz, byte[] bytes) {
                return null;
            }

            @Override
            public <T> byte[] serializer(T obj) {
                return new byte[0];
            }
        }
    }

    // Gson 中有些类型在默认情况下转化会报错，需要自己定义转化器来解决，这个转化器需要实现两个接口
    // JsonSerializer<T> 和 JsonDeserializer<T>
    class ClassCodec implements JsonSerializer<Class<?>>, JsonDeserializer<Class<?>> {

        @Override
        public Class<?> deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) {
            try {
                return Class.forName(jsonElement.getAsString());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public JsonElement serialize(Class aClass, Type type, JsonSerializationContext jsonSerializationContext) {
            return new JsonPrimitive(aClass.getName());
        }
    }
}
