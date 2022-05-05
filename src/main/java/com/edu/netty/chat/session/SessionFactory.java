package com.edu.netty.chat.session;

public class SessionFactory {
    private static Session session = new SessionImpl();

    public static Session getSession() {
        return session;
    }

}
