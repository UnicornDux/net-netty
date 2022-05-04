package com.edu.netty.message;

public class GroupMemberRequestMessage extends Message{

    @Override
    public int getMessageType() {
        return Message.GROUP_MEMBER_REQUEST_MESSAGE;
    }
}
