package com.edu.netty.message;

public class GroupChatRequestMessage extends Message{
    @Override
    public int getMessageType() {
        return Message.GROUP_CHAT_REQUEST_MESSAGE;
    }
}
