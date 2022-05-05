package com.edu.netty.chat.service;

public class UserServiceFactory {

    private static UserService userService = new UserServiceImpl();


    public static UserService getUserService() {
        return userService;
    }
}
