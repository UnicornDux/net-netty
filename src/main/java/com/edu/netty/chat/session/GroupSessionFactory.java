package com.edu.netty.chat.session;

public class GroupSessionFactory {

    private static GroupSession groupSession = new GroupSessionImpl();

    public static GroupSession getGroupSession() {
        return groupSession;
    }
}
