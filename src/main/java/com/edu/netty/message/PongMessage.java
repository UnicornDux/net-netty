package com.edu.netty.message;

public class PongMessage extends Message {

    @Override
    public int getMessageType() {
        return Message.PONG_MESSAGE;
    }
}
