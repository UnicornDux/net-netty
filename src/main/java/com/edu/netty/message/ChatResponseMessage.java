package com.edu.netty.message;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class ChatResponseMessage extends AbstractResponseMessage {

    private String from;
    private String content;

    public ChatResponseMessage(boolean success, String reason) {
        super(success, reason);
    }

    public ChatResponseMessage(String from, String content) {
        this.from = from;
        this.content = content;
    }

    @Override
    public int getMessageType() {
        return Message.CHAT_RESPONSE_MESSAGE;
    }
}
