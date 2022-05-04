package com.edu.netty.message;

public class GroupQuitResponseMessage extends Message{
    @Override
    public int getMessageType() {
        return Message.GROUP_QUIT_RESPONSE_MESSAGE;

    }
}
