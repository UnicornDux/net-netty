package com.edu.netty.message;

public class GroupCreateResponseMessage extends Message{
    @Override
    public int getMessageType() {
        return Message.GROUP_CREATE_RESPONSE_MESSAGE;
    }
}
