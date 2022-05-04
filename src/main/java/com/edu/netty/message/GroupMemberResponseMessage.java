package com.edu.netty.message;

public class GroupMemberResponseMessage extends Message{
    @Override
    public int getMessageType() {
        return Message.GROUP_MEMBER_RESPONSE_MESSAGE;
    }
}
