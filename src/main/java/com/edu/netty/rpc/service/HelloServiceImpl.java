package com.edu.netty.rpc.service;

public class HelloServiceImpl implements HelloService{

    @Override
    public String sayHello(String name) {
        return String.format("Hello %s", name);
    }
}
