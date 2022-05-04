package com.edu.netty.message;

public class GroupJoinRequestMessage extends Message {
    @Override
    public int getMessageType() {
        return Message.GROUP_JOIN_REQUEST_MESSAGE;
    }
}
