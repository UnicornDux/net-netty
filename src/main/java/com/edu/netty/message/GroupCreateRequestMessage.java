package com.edu.netty.message;

public class GroupCreateRequestMessage extends Message{
    @Override
    public int getMessageType() {
        return Message.GROUP_CREATE_REQUEST_MESSAGE;
    }
}
