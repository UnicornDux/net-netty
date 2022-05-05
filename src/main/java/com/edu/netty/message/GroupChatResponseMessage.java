package com.edu.netty.message;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class GroupChatResponseMessage extends AbstractResponseMessage{

    private String from;
    private String content;

    private String groupName;

    public GroupChatResponseMessage(boolean success, String reason) {
        super(success, reason);
    }

    public GroupChatResponseMessage(String from, String groupName, String content) {
        this.groupName = groupName;
        this.from = from;
        this.content = content;
    }

    @Override
    public int getMessageType() {
        return Message.GROUP_CHAT_RESPONSE_MESSAGE;
    }
}
