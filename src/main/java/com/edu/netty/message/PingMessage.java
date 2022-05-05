package com.edu.netty.message;

public class PingMessage extends Message {

    @Override
    public int getMessageType() {
        return Message.PING_MESSAGE;
    }
}
