package com.edu.netty.message;

public class GroupQuitRequestMessage extends Message{
    @Override
    public int getMessageType() {
        return Message.GROUP_QUIT_REQUEST_MESSAGE;
    }
}
