package com.edu.netty.message;

public class GroupChatResponseMessage extends Message{
    @Override
    public int getMessageType() {
        return Message.GROUP_CHAT_RESPONSE_MESSAGE;
    }
}
