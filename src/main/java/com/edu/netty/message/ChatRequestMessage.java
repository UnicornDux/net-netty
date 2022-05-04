package com.edu.netty.message;

public class ChatRequestMessage extends Message {

    @Override
    public int getMessageType() {
        return Message.CHAT_REQUEST_MESSAGE;
    }
}
