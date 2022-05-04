package com.edu.netty.message;

public class GroupJoinResponseMessage extends Message {
    @Override
    public int getMessageType() {
        return Message.GROUP_JOIN_RESPONSE_MESSAGE;
    }
}
