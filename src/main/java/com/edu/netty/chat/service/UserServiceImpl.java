package com.edu.netty.chat.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserServiceImpl implements UserService {


    private Map<String, String> allUserMap = new ConcurrentHashMap<>();

    {
        allUserMap.put("alex", "123");
        allUserMap.put("maria", "123");
        allUserMap.put("scott", "123");
        allUserMap.put("jack", "123");
        allUserMap.put("james", "123");
    }

    @Override
    public boolean login(String userName, String password) {
        return allUserMap.containsKey(userName) && allUserMap.get(userName).equals(password);
    }
}
