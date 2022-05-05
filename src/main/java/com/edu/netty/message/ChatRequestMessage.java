package com.edu.netty.message;

import lombok.Data;
import lombok.ToString;

@Data
@ToString(callSuper = true)
public class ChatRequestMessage extends Message {
    private String from;
    private String to;
    private String content;


    public ChatRequestMessage(String from, String to, String content) {
        this.content = content;
        this.to = to;
        this.from = from;
    }

    @Override
    public int getMessageType() {
        return Message.CHAT_REQUEST_MESSAGE;
    }
}
