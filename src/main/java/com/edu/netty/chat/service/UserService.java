package com.edu.netty.chat.service;

/**
 * 用户管理
 */
public interface UserService {

    /**
     *  登录
     * @param username
     * @param password
     * @return
     */
    boolean login(String username, String password);

}


