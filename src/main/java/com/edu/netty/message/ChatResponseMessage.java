package com.edu.netty.message;

public class ChatResponseMessage extends Message {
    @Override
    public int getMessageType() {
        return Message.CHAT_RESPONSE_MESSAGE;
    }
}
