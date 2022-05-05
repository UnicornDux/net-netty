package com.edu.netty.chat.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TestConcurrentHashMap {


    public static void main(String[] args) {

        Map<String, Object> map = new ConcurrentHashMap<>();

        // map.put("1", "1");

        Object o = map.putIfAbsent("1", "1");

        System.out.println(o);

    }
}
